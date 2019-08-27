/*
 * Copyright (C) 2019 Nafundi
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

package org.opendatakit.briefcase.operations.transfer.pull.filesystem;

import static java.util.stream.Collectors.toList;
import static org.opendatakit.briefcase.operations.transfer.pull.filesystem.FormInstaller.installForm;
import static org.opendatakit.briefcase.operations.transfer.pull.filesystem.FormInstaller.installSubmissions;
import static org.opendatakit.briefcase.reused.api.UncheckedFiles.stripFileExtension;
import static org.opendatakit.briefcase.reused.api.UncheckedFiles.walk;
import static org.opendatakit.briefcase.reused.job.Job.run;
import static org.opendatakit.briefcase.reused.model.form.FormMetadataCommands.upsert;
import static org.opendatakit.briefcase.reused.model.submission.SubmissionMetadataQueries.hasBeenAlreadyPulled;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import org.bushe.swing.event.EventBus;
import org.opendatakit.briefcase.operations.transfer.pull.PullEvent;
import org.opendatakit.briefcase.reused.api.Pair;
import org.opendatakit.briefcase.reused.job.Job;
import org.opendatakit.briefcase.reused.model.XmlElement;
import org.opendatakit.briefcase.reused.model.form.FormMetadata;
import org.opendatakit.briefcase.reused.model.form.FormMetadataPort;
import org.opendatakit.briefcase.reused.model.form.FormStatusEvent;
import org.opendatakit.briefcase.reused.model.submission.SubmissionLazyMetadata;
import org.opendatakit.briefcase.reused.model.submission.SubmissionMetadataPort;

public class PullFromCollectDir {
  private final Consumer<FormStatusEvent> onEventCallback;
  private final FormMetadataPort formMetadataPort;
  private final SubmissionMetadataPort submissionMetadataPort;

  public PullFromCollectDir(FormMetadataPort formMetadataPort, SubmissionMetadataPort submissionMetadataPort, Consumer<FormStatusEvent> onEventCallback) {
    this.onEventCallback = onEventCallback;
    this.formMetadataPort = formMetadataPort;
    this.submissionMetadataPort = submissionMetadataPort;
  }

  public Job<Void> pull(FormMetadata sourceFormMetadata, FormMetadata targetFormMetadata) {
    PullFromFileSystemTracker tracker = new PullFromFileSystemTracker(targetFormMetadata.getKey(), onEventCallback);

    return run(rs -> {
      tracker.trackStart();

      installForm(sourceFormMetadata, targetFormMetadata, tracker);

      List<Pair<Path, SubmissionLazyMetadata>> submissions = walk(sourceFormMetadata.getFormDir().getParent().resolve("instances"))
          .filter(p -> Files.isRegularFile(p)
              && p.getFileName().toString().startsWith(stripFileExtension(sourceFormMetadata.getFormFile()))
              && p.getFileName().toString().endsWith(".xml"))
          .map(submissionFile -> Pair.of(submissionFile, new SubmissionLazyMetadata(XmlElement.from(submissionFile))))
          .collect(toList());
      int totalSubmissions = submissions.size();

      List<Pair<Path, SubmissionLazyMetadata>> submissionsWithInstanceId = submissions.stream()
          .filter(pair -> pair.getRight().getInstanceId().isPresent()).collect(toList());
      int submissionsWithoutInstanceId = totalSubmissions - submissionsWithInstanceId.size();
      if (submissionsWithoutInstanceId > 0)
        tracker.trackSkippedSubmissionsWithoutInstanceId(submissionsWithoutInstanceId, totalSubmissions);

      List<Pair<Path, SubmissionLazyMetadata>> submissionsToPull = submissions.stream()
          .filter(pair -> !submissionMetadataPort.query(hasBeenAlreadyPulled(sourceFormMetadata.getKey().getId(), pair.getRight().getInstanceId().orElseThrow())))
          .collect(toList());
      int submissionsAlreadyPulled = submissionsWithInstanceId.size() - submissionsToPull.size();
      if (submissionsAlreadyPulled > 0)
        tracker.trackSkippedSubmissionsAlreadyPulled(submissionsAlreadyPulled, totalSubmissions);

      installSubmissions(targetFormMetadata, submissionsToPull, submissionMetadataPort, tracker);

      formMetadataPort.execute(upsert(targetFormMetadata));
      EventBus.publish(PullEvent.Success.of(targetFormMetadata.getKey()));

      tracker.trackEnd();
    });
  }

}
