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
package org.opendatakit.briefcase.delivery.cli;

import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;
import static org.opendatakit.briefcase.delivery.cli.Common.FORM_ID;
import static org.opendatakit.briefcase.delivery.cli.Common.STORAGE_DIR;
import static org.opendatakit.briefcase.operations.export.ExportForms.buildExportDateTimePrefix;
import static org.opendatakit.briefcase.reused.api.UncheckedFiles.createDirectories;
import static org.opendatakit.briefcase.reused.http.Http.DEFAULT_HTTP_CONNECTIONS;
import static org.opendatakit.briefcase.reused.model.form.FormMetadataQueries.lastCursorOf;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;
import org.opendatakit.briefcase.delivery.ui.export.ExportPanel;
import org.opendatakit.briefcase.operations.export.ExportConfiguration;
import org.opendatakit.briefcase.operations.export.ExportToCsv;
import org.opendatakit.briefcase.operations.export.ExportToGeoJson;
import org.opendatakit.briefcase.operations.transfer.pull.aggregate.Cursor;
import org.opendatakit.briefcase.operations.transfer.pull.aggregate.PullFromAggregate;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.opendatakit.briefcase.reused.cli.Args;
import org.opendatakit.briefcase.reused.cli.Operation;
import org.opendatakit.briefcase.reused.cli.Param;
import org.opendatakit.briefcase.reused.http.CommonsHttp;
import org.opendatakit.briefcase.reused.http.Http;
import org.opendatakit.briefcase.reused.job.Job;
import org.opendatakit.briefcase.reused.job.JobsRunner;
import org.opendatakit.briefcase.reused.model.DateRange;
import org.opendatakit.briefcase.reused.model.form.FormDefinition;
import org.opendatakit.briefcase.reused.model.form.FormMetadata;
import org.opendatakit.briefcase.reused.model.form.FormMetadataPort;
import org.opendatakit.briefcase.reused.model.form.FormStatusEvent;
import org.opendatakit.briefcase.reused.model.preferences.BriefcasePreferences;
import org.opendatakit.briefcase.reused.model.submission.SubmissionMetadataPort;
import org.opendatakit.briefcase.reused.model.transfer.AggregateServer;
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
  private static final Param<Void> SPLIT_SELECT_MULTIPLES = Param.flag("ssm", "split_select_multiples", "Split select multiple fields");
  private static final Param<Void> INCLUDE_GEOJSON_EXPORT = Param.flag("ig", "include_geojson", "Include a GeoJSON file with spatial data");
  private static final Param<Void> REMOVE_GROUP_NAMES = Param.flag("rgn", "remove_group_names", "Remove group names from column names");
  private static final Param<Void> SMART_APPEND = Param.flag("sa", "smart_append", "Include only new submissions since last export");

  public static Operation create(FormMetadataPort formMetadataPort, SubmissionMetadataPort submissionMetadataPort) {
    return Operation.of(
        EXPORT,
        args -> export(formMetadataPort, submissionMetadataPort, args),
        Arrays.asList(STORAGE_DIR, FORM_ID, FILE, EXPORT_DIR),
        Arrays.asList(PEM_FILE, EXCLUDE_MEDIA, OVERWRITE, START, END, PULL_BEFORE, SPLIT_SELECT_MULTIPLES, INCLUDE_GEOJSON_EXPORT, REMOVE_GROUP_NAMES, SMART_APPEND)
    );
  }

  private static void export(FormMetadataPort formMetadataPort, SubmissionMetadataPort submissionMetadataPort, Args args) {
    String formId = args.get(FORM_ID);
    Path exportDir = args.get(EXPORT_DIR);
    String baseFilename = args.get(FILE);
    boolean exportMedia = !args.has(EXCLUDE_MEDIA);
    boolean overwriteFiles = args.has(OVERWRITE);
    boolean startFromLast = args.has(PULL_BEFORE);
    Optional<LocalDate> startDate = args.getOptional(START);
    Optional<LocalDate> endDate = args.getOptional(END);
    Optional<Path> maybePemFile = args.getOptional(PEM_FILE);
    boolean splitSelectMultiples = args.has(SPLIT_SELECT_MULTIPLES);
    boolean includeGeoJson = args.has(INCLUDE_GEOJSON_EXPORT);
    boolean removeGroupNames = args.has(REMOVE_GROUP_NAMES);
    boolean smartAppend = args.has(SMART_APPEND);

    CliEventsCompanion.attach(log);
    BriefcasePreferences appPreferences = BriefcasePreferences.appScoped();
    BriefcasePreferences exportPrefs = BriefcasePreferences.forClass(ExportPanel.class);
    BriefcasePreferences pullPrefs = BriefcasePreferences.forClass(ExportPanel.class);

    int maxHttpConnections = appPreferences.getMaxHttpConnections().orElse(DEFAULT_HTTP_CONNECTIONS);
    Http http = appPreferences.getHttpProxy()
        .map(host -> CommonsHttp.of(maxHttpConnections, host))
        .orElseGet(() -> CommonsHttp.of(maxHttpConnections));

    Optional<FormMetadata> maybeFormStatus = formMetadataPort.fetchAll()
        .filter(formMetadata -> formMetadata.getKey().getId().equals(formId))
        .findFirst();

    createDirectories(exportDir);

    FormMetadata formMetadata = maybeFormStatus.orElseThrow(() -> new BriefcaseException("Form " + formId + " not found"));

    System.out.println("Exporting form " + formMetadata.getFormName() + " (" + formMetadata.getKey().getId() + ") to: " + exportDir);
    DateRange dateRange = new DateRange(startDate, endDate);
    ExportConfiguration configuration = ExportConfiguration.Builder.empty()
        .setExportFilename(baseFilename)
        .setExportDir(exportDir)
        .setPemFile(maybePemFile)
        .setDateRange(dateRange)
        .setPullBefore(startFromLast)
        .setOverwriteFiles(overwriteFiles)
        .setExportMedia(exportMedia)
        .setSplitSelectMultiples(splitSelectMultiples)
        .setIncludeGeoJsonExport(includeGeoJson)
        .setRemoveGroupNames(removeGroupNames)
        .setSmartAppend(smartAppend)
        .build();

    Job<Void> pullJob = Job.noOpSupplier();
    if (configuration.resolvePullBefore()) {
      Optional<AggregateServer> server = AggregateServer.readFromPrefs(appPreferences, pullPrefs, formMetadata.getKey());
      if (server.isPresent()) {
        Optional<Cursor> lastCursor = appPreferences.resolveStartFromLast()
            ? formMetadataPort.query(lastCursorOf(formMetadata.getKey()))
            : Optional.empty();

        pullJob = new PullFromAggregate(http, formMetadataPort, submissionMetadataPort, server.get(), false, Export::onEvent)
            .pull(formMetadata, lastCursor);
      }
    }

    FormDefinition formDef = FormDefinition.from(formMetadata);

    Job<Void> exportJob = Job.run(runnerStatus -> ExportToCsv.export(formMetadataPort, formMetadata, formDef, configuration));

    Job<Void> exportGeoJsonJob = configuration.resolveIncludeGeoJsonExport()
        ? Job.run(runnerStatus -> ExportToGeoJson.export(formMetadata, formDef, configuration))
        : Job.noOp;

    Job<Void> job = pullJob
        .thenRun(exportJob)
        .thenRun(exportGeoJsonJob)
        .thenRun(__ -> exportPrefs.put(
            buildExportDateTimePrefix(formMetadata.getKey().getId()),
            LocalDateTime.now().format(ISO_DATE_TIME)
        ));

    JobsRunner.launchAsync(job, Export::onError).waitForCompletion();
    System.out.println();
    System.out.println("All operations completed");
    System.out.println();
  }

  private static void onEvent(FormStatusEvent event) {
    System.out.println(event.getFormKey().getId() + " - " + event.getMessage());
  }

  private static void onError(Throwable e) {
    System.err.println("Error exporting a form: " + e.getMessage() + " (see the logs for more info)");
    log.error("Error exporting a form", e);
  }
}
