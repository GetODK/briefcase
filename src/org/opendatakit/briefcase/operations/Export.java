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
import static org.opendatakit.briefcase.operations.Common.bootCache;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;
import org.opendatakit.briefcase.export.ExportConfiguration;
import org.opendatakit.briefcase.export.ExportToCsv;
import org.opendatakit.briefcase.export.FormDefinition;
import org.opendatakit.briefcase.model.BriefcaseFormDefinition;
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.ui.export.ExportPanel;
import org.opendatakit.briefcase.util.FileSystemUtils;
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

  public static Operation EXPORT_FORM = Operation.of(
      EXPORT,
      args -> export(args.get(STORAGE_DIR),
          args.get(FORM_ID),
          args.get(EXPORT_DIR),
          args.get(FILE),
          !args.has(EXCLUDE_MEDIA),
          args.has(OVERWRITE),
          args.getOptional(START),
          args.getOptional(END),
          args.getOptional(PEM_FILE)
      ),
      Arrays.asList(STORAGE_DIR, FORM_ID, FILE, EXPORT_DIR),
      Arrays.asList(PEM_FILE, EXCLUDE_MEDIA, OVERWRITE, START, END)
  );

  public static void export(String storageDir, String formid, Path exportDir, String baseFilename, boolean exportMedia, boolean overwriteFiles, Optional<LocalDate> startDate, Optional<LocalDate> endDate, Optional<Path> maybePemFile) {
    CliEventsCompanion.attach(log);
    bootCache(storageDir);
    Optional<BriefcaseFormDefinition> maybeFormDefinition = FileSystemUtils.getBriefcaseFormList(BriefcasePreferences.buildBriefcaseDir(Paths.get(storageDir))).stream()
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
        Optional.empty(),
        Optional.empty(),
        Optional.of(overwriteFiles)
    );
    ExportToCsv.export(FormDefinition.from(formDefinition), configuration, exportMedia);

    BriefcasePreferences.forClass(ExportPanel.class).put(buildExportDateTimePrefix(formDefinition.getFormId()), LocalDateTime.now().format(ISO_DATE_TIME));
  }
}
