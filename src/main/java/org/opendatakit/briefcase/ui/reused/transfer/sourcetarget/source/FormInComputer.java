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

package org.opendatakit.briefcase.ui.reused.transfer.sourcetarget.source;

import static java.awt.Cursor.getPredefinedCursor;
import static org.opendatakit.briefcase.pull.FormInstaller.install;
import static org.opendatakit.briefcase.reused.job.Job.run;
import static org.opendatakit.briefcase.ui.reused.UI.errorMessage;
import static org.opendatakit.briefcase.ui.reused.UI.removeAllMouseListeners;

import java.awt.Container;
import java.awt.Cursor;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import javax.swing.JLabel;
import org.bushe.swing.event.EventBus;
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.model.OdkCollectFormDefinition;
import org.opendatakit.briefcase.model.form.FormMetadataPort;
import org.opendatakit.briefcase.pull.PullEvent;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.opendatakit.briefcase.reused.job.JobsRunner;
import org.opendatakit.briefcase.transfer.TransferForms;
import org.opendatakit.briefcase.ui.reused.FileChooser;
import org.opendatakit.briefcase.util.BadFormDefinition;

/**
 * Represents a filesystem location pointing to a form file as a source of forms for the Pull UI Panel.
 */
public class FormInComputer implements PullSource<FormStatus> {
  private final Consumer<PullSource> consumer;
  private Path path;
  private FormStatus form;

  FormInComputer(Consumer<PullSource> consumer) {
    this.consumer = consumer;
  }

  @Override
  public void onSelect(Container container) {
    Optional<Path> selectedFile = FileChooser
        .file(container, Optional.empty(), file -> Files.isDirectory(file.toPath()) ||
            (Files.isRegularFile(file.toPath()) && file.toPath().getFileName().toString().endsWith(".xml")), "XML file")
        .choose()
        // TODO Changing the FileChooser to handle Paths instead of Files would improve this code and it's also coherent with the modernization (use NIO2 API) of this basecode
        .map(File::toPath);

    // Shortcircuit if the user has cancelled
    if (!selectedFile.isPresent())
      return;

    try {
      path = selectedFile.get();
      set(new FormStatus(new OdkCollectFormDefinition(path.toFile())));
    } catch (BadFormDefinition e) {
      errorMessage("Wrong file", "Bad form definition file. Please select another file.");
    }
  }

  @Override
  public void set(FormStatus form) {
    this.form = form;
    consumer.accept(this);
  }

  @Override
  public boolean accepts(Object o) {
    return o instanceof Path;
  }

  @Override
  public List<FormStatus> getFormList() {
    return Collections.singletonList(form);
  }

  @Override
  public void storeSourcePrefs(BriefcasePreferences prefs, boolean storePasswords) {
    // No prefs to store
  }

  @Override
  public JobsRunner pull(TransferForms forms, BriefcasePreferences appPreferences, FormMetadataPort formMetadataPort) {
    return JobsRunner.launchAsync(run(jobStatus ->
        install(appPreferences.getBriefcaseDir().orElseThrow(BriefcaseException::new), form)
    )).onComplete(() -> EventBus.publish(new PullEvent.PullComplete()));
  }

  @Override
  public boolean canBeReloaded() {
    return false;
  }

  @Override
  public String getDescription() {
    return String.format("%s at %s", form.getFormName(), path.toString());
  }

  @Override
  public void decorate(JLabel label) {
    label.setText(getDescription());
    label.setCursor(getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    removeAllMouseListeners(label);
  }

  @Override
  public String toString() {
    return "Form definition";
  }
}
