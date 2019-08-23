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

import static org.opendatakit.briefcase.export.SubmissionParser.parseSubmission;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

class ExportTools {
  static Stream<Submission> getValidSubmissions(FormDefinition formDef, ExportConfiguration configuration, List<Path> submissionFiles, SubmissionExportErrorCallback onParsingError, SubmissionExportErrorCallback onInvalidSubmission) {
    return submissionFiles.parallelStream()
        // Parse the submission and leave only those OK to be exported
        .map(path -> parseSubmission(path, formDef.isFileEncryptedForm(), configuration.getPrivateKey(), onParsingError))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .filter(submission -> {
          boolean valid = submission.isValid(formDef.hasRepeatableFields());
          if (!valid)
            onInvalidSubmission.accept(submission.getPath(), "invalid submission");
          return valid;
        });
  }
}
