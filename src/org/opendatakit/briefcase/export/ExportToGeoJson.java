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

package org.opendatakit.briefcase.export;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import static java.util.stream.Collectors.groupingByConcurrent;
import static java.util.stream.Collectors.reducing;
import static java.util.stream.Collectors.toList;
import static org.opendatakit.briefcase.export.ExportOutcome.ALL_EXPORTED;
import static org.opendatakit.briefcase.export.ExportOutcome.ALL_SKIPPED;
import static org.opendatakit.briefcase.export.ExportOutcome.SOME_SKIPPED;
import static org.opendatakit.briefcase.export.SubmissionParser.getListOfSubmissionFiles;
import static org.opendatakit.briefcase.reused.UncheckedFiles.copy;
import static org.opendatakit.briefcase.reused.UncheckedFiles.createDirectories;
import static org.opendatakit.briefcase.reused.UncheckedFiles.deleteRecursive;
import static org.opendatakit.briefcase.reused.UncheckedFiles.exists;
import static org.opendatakit.briefcase.reused.UncheckedFiles.write;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.bushe.swing.event.EventBus;
import org.opendatakit.briefcase.ui.reused.Analytics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExportToGeoJson {
  private static final Logger log = LoggerFactory.getLogger(ExportToGeoJson.class);

  /**
   * @see #export(FormDefinition, ExportConfiguration, Optional)
   */
  public static ExportOutcome export(FormDefinition formDef, ExportConfiguration configuration) {
    return export(formDef, configuration, Optional.empty());
  }

  /**
   * @see #export(FormDefinition, ExportConfiguration, Optional)
   */
  public static ExportOutcome export(FormDefinition formDef, ExportConfiguration configuration, Analytics analytics) {
    return export(formDef, configuration, Optional.of(analytics));
  }

  /**
   * Export a form's submissions into some CSV files.
   * <p>
   * If the form has repeat groups, each repeat group will be exported into a separate CSV file.
   *
   * @param formDef       the {@link FormDefinition} form definition of the form to be exported
   * @param configuration the {@link ExportConfiguration} export configuration
   * @return an {@link ExportOutcome} with the export operation's outcome
   * @see ExportConfiguration
   */
  private static ExportOutcome export(FormDefinition formDef, ExportConfiguration configuration, Optional<Analytics> analytics) {
    // Create an export tracker object with the total number of submissions we have to export
    ExportProcessTracker exportTracker = new ExportProcessTracker(formDef);
    exportTracker.start();

    SubmissionExportErrorCallback onParsingError = buildParsingErrorCallback(configuration.getErrorsDir(formDef.getFormName()));

    List<Path> submissionFiles = getListOfSubmissionFiles(formDef, configuration.getDateRange(), onParsingError);
    exportTracker.trackTotal(submissionFiles.size());

    createDirectories(configuration.getExportDir());

    // Prepare the list of csv files we will export:
    //  - one for the main instance
    //  - one for each repeat group
    List<Csv> csvs = new ArrayList<>();
    csvs.add(Csv.main(formDef, configuration));
    csvs.addAll(formDef.getRepeatableFields().stream()
        .map(groupModel -> Csv.repeat(formDef, groupModel, configuration))
        .collect(toList()));

    csvs.forEach(Csv::prepareOutputFiles);

    if (formDef.getModel().hasAuditField()) {
      Path audit = configuration.getAuditPath(formDef.getFormName());
      if (!exists(audit) || configuration.resolveOverwriteExistingFiles())
        write(audit, "instance ID, event, node, start, end\n", CREATE, WRITE, TRUNCATE_EXISTING);
    }

    // Generate csv lines grouped by the fqdn of the model they belong to
    Stream<Submission> validSubmissions = ExportTools.getValidSubmissions(formDef, configuration, analytics, onParsingError, submissionFiles);

    Map<String, CsvLines> csvLinesPerModel = validSubmissions
        // Track the submission
        .peek(s -> exportTracker.incAndReport())
        // Use the mapper of each Csv instance to map the submission into their respective outputs
        .flatMap(submission -> csvs.stream()
            .map(Csv::getMapper)
            .map(mapper -> mapper.apply(submission)))
        // Group and merge the CsvLines by the model they belong to
        .collect(groupingByConcurrent(
            CsvLines::getModelFqn,
            reducing(CsvLines.empty(), CsvLines::merge)
        ));

    // TODO We should have an extra step to produce the side effect of writing media files to disk to avoid having side-effects while generating the CSV output of binary fields

    // Write lines to each output Csv
    csvs.forEach(csv -> csv.appendLines(
        Optional.ofNullable(csvLinesPerModel.get(csv.getModelFqn())).orElse(CsvLines.empty())
    ));

    exportTracker.end();

    ExportOutcome exportOutcome = exportTracker.computeOutcome();
    if (exportOutcome == ALL_EXPORTED)
      EventBus.publish(ExportEvent.successForm(formDef, (int) exportTracker.total));

    if (exportOutcome == SOME_SKIPPED)
      EventBus.publish(ExportEvent.partialSuccessForm(formDef, (int) exportTracker.exported, (int) exportTracker.total));

    if (exportOutcome == ALL_SKIPPED)
      EventBus.publish(ExportEvent.failure(formDef, "All submissions have been skipped"));

    return exportOutcome;
  }

  private static SubmissionExportErrorCallback buildParsingErrorCallback(Path errorsDir) {
    AtomicInteger errorSeq = new AtomicInteger(1);
    // Remove errors from a previous export attempt
    if (exists(errorsDir))
      deleteRecursive(errorsDir);
    return (path, message) -> {
      if (!exists(errorsDir))
        createDirectories(errorsDir);
      copy(path, errorsDir.resolve("failed_submission_" + errorSeq.getAndIncrement() + ".xml"));
      log.warn("A submission has been excluded from the export output due to some problem ({}). If you didn't expect this, please ask for support at https://forum.opendatakit.org/c/support", message);
    };
  }

}
