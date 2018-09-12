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
package org.opendatakit.briefcase.ui.export.components;

import java.util.Optional;
import java.util.function.Consumer;
import org.opendatakit.briefcase.export.ExportConfiguration;

public class ConfigurationDialog {
  final ConfigurationDialogForm form;
  private final ConfigurationPanel confPanel;

  private ConfigurationDialog(ConfigurationDialogForm form, ConfigurationPanel confPanel) {
    this.form = form;
    this.confPanel = confPanel;

    if (!confPanel.isEmpty())
      form.enableClearAll();

    if (!confPanel.isValid())
      form.disableOK();

    confPanel.onChange(() -> {
      if (!confPanel.getConfiguration().isEmpty())
        form.enableClearAll();
      else
        form.disableClearAll();

      if (this.confPanel.isValid())
        form.enableOK();
      else
        form.disableOK();
    });
  }

  static ConfigurationDialog overridePanel(Optional<ExportConfiguration> configuration, String formName, boolean hasTransferSettings, boolean savePasswordsConsent) {
    ConfigurationPanel confPanel = ConfigurationPanel.overridePanel(configuration.orElse(ExportConfiguration.empty()), savePasswordsConsent, hasTransferSettings);
    ConfigurationDialogForm form = new ConfigurationDialogForm(confPanel.getForm(), "Override " + formName + " Export Configuration");
    return new ConfigurationDialog(form, confPanel);
  }

  public static ConfigurationDialog defaultPanel(Optional<ExportConfiguration> configuration, boolean savePasswordsConsent) {
    ConfigurationPanel confPanel = ConfigurationPanel.defaultPanel(configuration.orElse(ExportConfiguration.empty()), savePasswordsConsent);
    ConfigurationDialogForm form = new ConfigurationDialogForm(confPanel.getForm(), "Default Export Configuration");
    return new ConfigurationDialog(form, confPanel);
  }

  public void onOK(Consumer<ExportConfiguration> callback) {
    form.onOK(() -> callback.accept(confPanel.getConfiguration()));
  }

  public void onRemove(Runnable callback) {
    form.onRemove(callback);
  }

  public void open() {
    form.open();
  }

  ConfigurationPanel getConfPanel() {
    return confPanel;
  }
}
