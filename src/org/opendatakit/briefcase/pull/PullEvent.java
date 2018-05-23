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

package org.opendatakit.briefcase.pull;

import java.util.List;
import java.util.Optional;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.model.ServerConnectionInfo;

public class PullEvent {

  public static class Failure extends PullEvent {
    public Failure() {
    }
  }

  public static class Success extends PullEvent {
    public final List<FormStatus> forms;
    public final Optional<ServerConnectionInfo> transferSettings;

    public Success(List<FormStatus> forms, ServerConnectionInfo transferSettings) {
      this.forms = forms;
      this.transferSettings = Optional.ofNullable(transferSettings);
    }
  }

  public static class Abort extends PullEvent {
    public final String cause;

    public Abort(String cause) {
      this.cause = cause;
    }
  }

  public static class NewForm extends PullEvent {
    public final FormStatus form;
    public final Optional<ServerConnectionInfo> transferSettings;

    public NewForm(FormStatus form, ServerConnectionInfo transferSettings) {
      this.form = form;
      this.transferSettings = Optional.ofNullable(transferSettings);
    }
  }
}
