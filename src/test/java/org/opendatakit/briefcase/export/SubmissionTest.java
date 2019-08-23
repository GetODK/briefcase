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

import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.hamcrest.Matchers;
import org.junit.Test;

@SuppressWarnings({"OptionalGetWithoutIsPresent", "checkstyle:ParameterName"})
public class SubmissionTest {

  private static final SubmissionExportErrorCallback NO_OP = (__, ___) -> {
  };

  @Test
  public void it_is_valid_if_it_has_instance_id() throws IOException {
    Path path = Files.createTempFile("submission_", ".xml");
    Files.write(path, "<data id=\"simple-form\" instanceID=\"123456789\" xmlns=\"http://opendatakit.org/submissions\"><field>value</field></data>".getBytes());
    Submission sub = SubmissionParser.parseSubmission(path, false, Optional.empty(), NO_OP).get();
    assertThat(sub.isValid(false), Matchers.is(true));
  }

  @Test
  public void it_is_valid_if_it_does_not_have_instance_id_and_it_does_not_have_repeat_groups() throws IOException {
    Path path = Files.createTempFile("submission_", ".xml");
    Files.write(path, "<data id=\"simple-form\" xmlns=\"http://opendatakit.org/submissions\"><field>value</field></data>".getBytes());
    Submission sub = SubmissionParser.parseSubmission(path, false, Optional.empty(), NO_OP).get();
    assertThat(sub.isValid(false), Matchers.is(true));
  }

  @Test
  public void it_is_not_valid_if_it_does_not_have_instance_id_and_it_has_repeat_groups() throws IOException {
    Path path = Files.createTempFile("submission_", ".xml");
    Files.write(path, "<data id=\"simple-form\" xmlns=\"http://opendatakit.org/submissions\"><field>value</field></data>".getBytes());
    Submission sub = SubmissionParser.parseSubmission(path, false, Optional.empty(), NO_OP).get();
    assertThat(sub.isValid(true), Matchers.is(false));
  }
}
