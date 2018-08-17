/*
 * Copyright (C) 2018 Nafundi
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.opendatakit.briefcase.operations;

import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;
import static org.opendatakit.briefcase.export.ExportForms.buildExportDateTimePrefix;
import static org.opendatakit.briefcase.operations.Common.FORM_ID;
import static org.opendatakit.briefcase.operations.Common.STORAGE_DIR;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import org.opendatakit.briefcase.export.ExportConfiguration;
import org.opendatakit.briefcase.export.ExportToCsv;
import org.opendatakit.briefcase.export.FormDefinition;
import org.opendatakit.briefcase.model.BriefcaseFormDefinition;
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.model.ServerConnectionInfo;
import org.opendatakit.briefcase.model.TerminationFuture;
import org.opendatakit.briefcase.reused.OverridableBoolean;
import org.opendatakit.briefcase.transfer.NewTransferAction;
import org.opendatakit.briefcase.ui.export.ExportPanel;
import org.opendatakit.briefcase.util.FormCache;
import org.opendatakit.common.cli.Operation;
import org.opendatakit.common.cli.Param;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Export {
  private static final Logger log = LoggerFactory.getLogger(Export.class);
  private static final Param<Void> EXPORT = Param.flag("e", "export", "Export a form");
  private static final Param<Path> EXPORT_DIR = Param.arg("ed", "export_directory", "Export directory", Paths::get);
  private static final Param<String> FILE = Param.arg("f", "export_filename", "Filename for export operation");
  private static final Param<LocalDate> START = Param.localDate("start", "export_start_date", "Export start date (inclusive)");
  private static final Param<LocalDate> END = Param.localDate("end", "export_end_date", "Export end date (inclusive)");
  private static final Param<Void> EXCLUDE_MEDIA = Param.flag("em", "exclude_media_export", "Exclude media in export");
  private static final Param<Void> OVERWRITE = Param.flag("oc", "overwrite_csv_export", "Overwrite files during export");
  private static final Param<Path> PEM_FILE = Param.arg("pf", "pem_file", "PEM file for form decryption", Paths::get);
  private static final Param<Void> PULL_BEFORE = Param.flag("pb", "pull_before", "Pull before export");
  // Externally, we use "split select multiples" to conform to language used in other apps
  // Internally, we use "explode" and "choice lists" to conform to JavaRosa's internal domain
  private static final Param<Void> EXPLODE_CHOICE_LISTS = Param.flag("ssm", "split_select_multiples", "Split select multiple fields");

  public static Operation EXPORT_FORM = Operation.of(
      EXPORT,
      args -> export(args.get(STORAGE_DIR),
          args.get(FORM_ID),
          args.get(EXPORT_DIR),
          args.get(FILE),
          !args.has(EXCLUDE_MEDIA),
          args.has(OVERWRITE),
          args.has(PULL_BEFORE),
          args.getOptional(START),
          args.getOptional(END),
          args.getOptional(PEM_FILE),
          args.has(EXPLODE_CHOICE_LISTS)
      ),
      Arrays.asList(STORAGE_DIR, FORM_ID, FILE, EXPORT_DIR),
      Arrays.asList(PEM_FILE, EXCLUDE_MEDIA, OVERWRITE, START, END, PULL_BEFORE, EXPLODE_CHOICE_LISTS)
  );

  public static void export(String storageDir, String formid, Path exportDir, String baseFilename, boolean exportMedia, boolean overwriteFiles, boolean pullBefore, Optional<LocalDate> startDate, Optional<LocalDate> endDate, Optional<Path> maybePemFile, boolean explodeChoiceLists) {
    CliEventsCompanion.attach(log);
    Path briefcaseDir = Common.getOrCreateBriefcaseDir(storageDir);
    FormCache formCache = FormCache.from(briefcaseDir);
    formCache.update();
    Optional<BriefcaseFormDefinition> maybeFormDefinition = formCache.getForms().stream()
        .filter(form -> form.getFormId().equals(formid))
        .findFirst();

    BriefcaseFormDefinition formDefinition = maybeFormDefinition.orElseThrow(() -> new FormNotFoundException(formid));

    System.out.println("Exporting form " + formDefinition.getFormName() + " (" + formDefinition.getFormId() + ") to: " + exportDir);
    ExportConfiguration configuration = new ExportConfiguration(
        Optional.of(baseFilename),
        Optional.of(exportDir),
        maybePemFile,
        startDate,
        endDate,
        OverridableBoolean.of(pullBefore),
        OverridableBoolean.of(overwriteFiles),
        OverridableBoolean.of(exportMedia),
        Optional.of(explodeChoiceLists)
    );

    if (configuration.resolvePullBefore()) {
      BriefcasePreferences appPreferences = BriefcasePreferences.appScoped();
      FormStatus formStatus = new FormStatus(FormStatus.TransferType.EXPORT, formDefinition);

      String urlKey = String.format("%s_pull_settings_url", formid);
      String usernameKey = String.format("%s_pull_settings_username", formid);
      String passwordKey = String.format("%s_pull_settings_password", formid);

      if (appPreferences.hasKey(urlKey) && appPreferences.hasKey(usernameKey) && appPreferences.hasKey(passwordKey)) {
        ServerConnectionInfo transferSettings = new ServerConnectionInfo(
            appPreferences.nullSafeGet(urlKey)
                .orElseThrow(() -> new RuntimeException("Null value saved for " + urlKey)), appPreferences.nullSafeGet(usernameKey)
            .orElseThrow(() -> new RuntimeException("Null value saved for " + usernameKey)),
            appPreferences.nullSafeGet(passwordKey)
                .orElseThrow(() -> new RuntimeException("Null value saved for " + passwordKey)).toCharArray()
        );

        NewTransferAction.transferServerToBriefcase(
            transferSettings,
            new TerminationFuture(),
            Collections.singletonList(formStatus),
            briefcaseDir,
            appPreferences.getPullInParallel().orElse(false)
        );
      }

    }

    ExportToCsv.export(FormDefinition.from(formDefinition), configuration);

    BriefcasePreferences.forClass(ExportPanel.class).put(buildExportDateTimePrefix(formDefinition.getFormId()), LocalDateTime.now().format(ISO_DATE_TIME));
  }
}
