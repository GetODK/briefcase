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

package org.opendatakit.briefcase.pull.filesystem;

import static org.opendatakit.briefcase.model.form.FormMetadataCommands.upsert;
import static org.opendatakit.briefcase.pull.filesystem.FormInstaller.installForm;
import static org.opendatakit.briefcase.reused.job.Job.run;

import java.util.function.Consumer;
import org.bushe.swing.event.EventBus;
import org.opendatakit.briefcase.model.FormStatusEvent;
import org.opendatakit.briefcase.model.form.FormMetadata;
import org.opendatakit.briefcase.model.form.FormMetadataPort;
import org.opendatakit.briefcase.pull.PullEvent;
import org.opendatakit.briefcase.reused.job.Job;

public class PullFormDefinition {
  private final Consumer<FormStatusEvent> onEventCallback;
  private final FormMetadataPort formMetadataPort;

  public PullFormDefinition(FormMetadataPort formMetadataPort, Consumer<FormStatusEvent> onEventCallback) {
    this.onEventCallback = onEventCallback;
    this.formMetadataPort = formMetadataPort;
  }

  public Job<Void> pull(FormMetadata sourceFormMetadata, FormMetadata targetFormMetadata) {
    PullFromFileSystemTracker tracker = new PullFromFileSystemTracker(targetFormMetadata, onEventCallback);

    return run(rs -> tracker.trackStart())
        .thenRun(rs -> installForm(sourceFormMetadata, targetFormMetadata, tracker))
        .thenRun(rs -> {
          formMetadataPort.execute(upsert(targetFormMetadata));
          EventBus.publish(PullEvent.Success.of(targetFormMetadata.getKey()));
        })
        .thenRun(rs -> tracker.trackEnd());
  }
}
