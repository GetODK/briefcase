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

package org.opendatakit.briefcase.pull.filesystem;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.util.stream.Collectors.toList;
import static org.opendatakit.briefcase.reused.UncheckedFiles.copy;
import static org.opendatakit.briefcase.reused.UncheckedFiles.createDirectories;
import static org.opendatakit.briefcase.reused.UncheckedFiles.deleteRecursive;
import static org.opendatakit.briefcase.reused.UncheckedFiles.exists;
import static org.opendatakit.briefcase.reused.UncheckedFiles.list;
import static org.opendatakit.briefcase.reused.UncheckedFiles.walk;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.opendatakit.briefcase.export.SubmissionMetaData;
import org.opendatakit.briefcase.export.XmlElement;
import org.opendatakit.briefcase.model.form.FormMetadata;
import org.opendatakit.briefcase.reused.Pair;

public class FormInstaller {
  static void installForm(FormMetadata sourceFormMetadata, FormMetadata targetFormMetadata, PullFromFileSystemTracker tracker) {
    if (!exists(targetFormMetadata.getFormDir()))
      createDirectories(targetFormMetadata.getFormDir());

    copy(sourceFormMetadata.getFormFile(), targetFormMetadata.getFormFile(), REPLACE_EXISTING);
    tracker.trackFormInstalled();

    if (exists(targetFormMetadata.getFormMediaDir()))
      deleteRecursive(targetFormMetadata.getFormMediaDir());

    if (exists(sourceFormMetadata.getFormMediaDir())) {
      createDirectories(targetFormMetadata.getFormMediaDir());

      List<Path> attachments = walk(sourceFormMetadata.getFormMediaDir())
          .filter(Files::isRegularFile)
          .collect(toList());
      AtomicInteger attachmentSeq = new AtomicInteger(1);
      int totalAttachments = attachments.size();

      attachments
          .forEach(sourceMediaFile -> {
            copy(sourceMediaFile, targetFormMetadata.getFormMediaFile(sourceMediaFile.getFileName().toString()), REPLACE_EXISTING);
            tracker.trackFormAttachmentInstaller(attachmentSeq.getAndIncrement(), totalAttachments);
          });
    }
  }

  static void installSubmissions(FormMetadata formMetadata, List<Pair<Path, SubmissionMetaData>> submissions, PullFromFileSystemTracker tracker) {
    int totalSubmissions = submissions.size();
    AtomicInteger submissionSeq = new AtomicInteger(1);

    submissions.forEach(pair -> {
      Path submissionFile = pair.getLeft();
      String instanceId = pair.getRight().getInstanceId().orElseThrow();
      createDirectories(formMetadata.getSubmissionDir(instanceId));
      copy(submissionFile, formMetadata.getSubmissionFile(instanceId), REPLACE_EXISTING);
      int submissionNumber = submissionSeq.getAndIncrement();
      tracker.trackSubmissionInstalled(submissionNumber, totalSubmissions);

      List<Path> attachments = list(submissionFile.getParent())
          .filter(p -> !p.equals(submissionFile))
          .collect(toList());

      int totalAttachments = attachments.size();
      AtomicInteger attachmentSeq = new AtomicInteger(1);

      attachments.forEach(attachment -> {
        copy(attachment, formMetadata.getSubmissionMediaFile(instanceId, attachment.getFileName().toString()));
        tracker.trackSubmissionAttachmentInstalled(submissionNumber, totalSubmissions, attachmentSeq.getAndIncrement(), totalAttachments);
      });
    });
  }

  public static List<FormMetadata> scanCollectFormsAt(Path path) {
    return walk(path)
        .filter(file -> Files.isRegularFile(file)
            && !file.getFileName().toString().equals("submission.xml")
            && file.getFileName().toString().endsWith(".xml"))
        .filter(file -> isAForm(XmlElement.from(file)))
        .map(FormMetadata::from)
        .collect(toList());
  }

  private static boolean isAForm(XmlElement root) {
    return root.getName().equals("html")
        && root.findElements("head", "title").size() == 1
        && root.findElements("head", "model", "instance").size() >= 1
        && root.findElements("body").size() == 1;
  }
}
