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

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.opendatakit.briefcase.reused.UncheckedFiles.copy;
import static org.opendatakit.briefcase.reused.UncheckedFiles.createDirectories;
import static org.opendatakit.briefcase.reused.UncheckedFiles.deleteRecursive;
import static org.opendatakit.briefcase.reused.UncheckedFiles.exists;
import static org.opendatakit.briefcase.reused.UncheckedFiles.walk;
import static org.opendatakit.briefcase.util.StringUtils.stripIllegalChars;

import java.nio.file.Path;
import org.bushe.swing.event.EventBus;
import org.opendatakit.briefcase.model.FormStatusEvent;
import org.opendatakit.briefcase.model.form.FormMetadata;

/**
 * This class has UI/CLI independent methods to install forms into
 * Briefcase's Storage Directory.
 * <p>
 * The goal of this class is to be as decoupled as we can to make
 * testing these business critical operations easier.
 */
public class FormInstaller {
  /**
   * Takes a form's metadata and installs the form definition file and any
   * related media files into Briefcase's Storage Directory.
   * <p>
   * After installing the form, it updates the form cache.
   * <p>
   * This method won't install any submission.
   */
  public static void install(Path briefcaseDir, FormMetadata formMetadata) {
    installForm(briefcaseDir, formMetadata);
    EventBus.publish(PullEvent.Success.of(formMetadata.getKey()));
  }

  private static void installForm(Path briefcaseDir, FormMetadata sourceFormMetadata) {
    // Get and prepare target form directory inside our Storage Directory
    Path targetFormDir = briefcaseDir.resolve("forms").resolve(stripIllegalChars(sourceFormMetadata.getKey().getName()));
    if (!exists(targetFormDir))
      createDirectories(targetFormDir);

    // Install the form definition file, changing the filename on the process
    Path targetFormFile = targetFormDir.resolve(sourceFormMetadata.getKey().getName() + ".xml");
    copy(sourceFormMetadata.getFormFile(), targetFormFile, REPLACE_EXISTING);
    EventBus.publish(new FormStatusEvent(sourceFormMetadata.getKey(), "Installed form definition file"));

    // Check if there is a media directory to install as well
    Path sourceMediaDir = sourceFormMetadata.getFormFile().resolveSibling(sourceFormMetadata.getKey().getName() + "-media");
    Path targetMediaDir = targetFormDir.resolve(sourceFormMetadata.getKey().getName() + "-media");
    if (exists(targetMediaDir))
      deleteRecursive(targetMediaDir);
    if (exists(sourceMediaDir))
      walk(sourceMediaDir)
          .forEach(sourcePath -> {
            copy(sourcePath, targetMediaDir.resolve(sourceMediaDir.relativize(sourcePath)));
            EventBus.publish(new FormStatusEvent(sourceFormMetadata.getKey(), "Installed " + sourcePath.getFileName() + " media file"));
          });

    EventBus.publish(new FormStatusEvent(sourceFormMetadata.getKey(), "Success"));
  }
}
