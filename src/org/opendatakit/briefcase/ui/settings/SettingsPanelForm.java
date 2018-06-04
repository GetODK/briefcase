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

package org.opendatakit.briefcase.ui.settings;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import org.apache.http.HttpHost;
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.reused.OptionalProduct;
import org.opendatakit.briefcase.ui.reused.FileChooser;

@SuppressWarnings("checkstyle:MethodName")
public class SettingsPanelForm {
  public JPanel container;
  private JPanel storageLocationContainer;
  private JLabel storageLocationLabel;
  private JTextField storageLocationField;
  private JPanel storageLocationButtons;
  private JButton storageLocationClearButton;
  private JButton storageLocationChooseButton;
  private JCheckBox pullInParallelField;
  private JCheckBox rememberPasswordsField;
  private JCheckBox sendUsageDataField;
  private JCheckBox useHttpProxyField;
  private JTextField httpProxyHostField;
  private JSpinner httpProxyPortField;
  private JLabel httpProxyPortLabel;
  private JLabel httpProxyJostLabel;
  private final List<Consumer<Path>> onStorageLocationCallbacks = new ArrayList<>();
  private final List<Runnable> onClearStorageLocationCallbacks = new ArrayList<>();
  private final List<Consumer<HttpHost>> onHttpProxyCallbacks = new ArrayList<>();
  private final List<Runnable> onClearHttpProxyCallbacks = new ArrayList<>();

  public SettingsPanelForm() {
    httpProxyPortField = new JIntegerSpinner(8080, 0, 65535, 1);
    $$$setupUI$$$();

    storageLocationChooseButton.addActionListener(__ -> FileChooser.directory(container, Optional.empty())
        .choose()
        .ifPresent(path -> setStorageLocation(path.toPath())));

    storageLocationClearButton.addActionListener(__ -> this.clearStorageLocation());

    useHttpProxyField.addActionListener(__ -> {
      if (!useHttpProxyField.isSelected())
        unsetHttpProxy();
      updateProxyFields();
    });

    httpProxyHostField.addFocusListener(onFocusLost(() -> OptionalProduct.all(
        Optional.ofNullable(httpProxyHostField.getText()),
        Optional.ofNullable(httpProxyPortField.getValue()).map(o -> (Integer) o)
    ).map(HttpHost::new).ifPresent(this::setHttpProxy)));

    httpProxyPortField.addChangeListener(__ -> OptionalProduct.all(
        Optional.ofNullable(httpProxyHostField.getText()).filter(s -> !s.isEmpty()),
        Optional.ofNullable(httpProxyPortField.getValue()).map(o -> (Integer) o)
    ).map(HttpHost::new).ifPresent(this::setHttpProxy));

    updateProxyFields();
  }

  private void updateProxyFields() {
    httpProxyHostField.setEnabled(useHttpProxyField.isSelected());
    httpProxyPortField.setEnabled(useHttpProxyField.isSelected());
  }

  void setStorageLocation(Path path) {
    storageLocationField.setText(BriefcasePreferences.buildBriefcaseDir(path).toString());
    storageLocationChooseButton.setVisible(false);
    storageLocationClearButton.setVisible(true);
    onStorageLocationCallbacks.forEach(consumer -> consumer.accept(path));
  }

  private void clearStorageLocation() {
    storageLocationField.setText("");
    storageLocationChooseButton.setVisible(true);
    storageLocationClearButton.setVisible(false);
    onClearStorageLocationCallbacks.forEach(Runnable::run);
  }

  void onStorageLocation(Consumer<Path> onSet, Runnable onClear) {
    onStorageLocationCallbacks.add(onSet);
    onClearStorageLocationCallbacks.add(onClear);
  }

  void setHttpProxy(HttpHost proxy) {
    useHttpProxyField.setSelected(true);
    httpProxyHostField.setText(proxy.getHostName());
    httpProxyPortField.setValue(proxy.getPort());
    onHttpProxyCallbacks.forEach(callback -> callback.accept(proxy));
    updateProxyFields();
  }

  private void unsetHttpProxy() {
    httpProxyHostField.setText("");
    httpProxyPortField.setValue(8080);
    onClearHttpProxyCallbacks.forEach(Runnable::run);
  }

  void onHttpProxy(Consumer<HttpHost> onSet, Runnable onClear) {
    onHttpProxyCallbacks.add(onSet);
    onClearHttpProxyCallbacks.add(onClear);
  }

  private FocusListener onFocusLost(Runnable callback) {
    return new FocusAdapter() {
      @Override
      public void focusLost(FocusEvent e) {
        super.focusLost(e);
        callback.run();
      }
    };
  }


  void onPullInParallelChange(Consumer<Boolean> callback) {
    pullInParallelField.addActionListener(__ -> callback.accept(pullInParallelField.isSelected()));
  }

  void setPullInParallel(Boolean enabled) {
    pullInParallelField.setSelected(enabled);
  }

  void onRememberPasswordsChange(Consumer<Boolean> callback) {
    rememberPasswordsField.addActionListener(__ -> callback.accept(rememberPasswordsField.isSelected()));
  }

  void setRememberPasswords(Boolean enabled) {
    rememberPasswordsField.setSelected(enabled);
  }

  void onSendUsageDataChange(Consumer<Boolean> callback) {
    sendUsageDataField.addActionListener(__ -> callback.accept(sendUsageDataField.isSelected()));
  }

  void setSendUsageData(Boolean enabled) {
    sendUsageDataField.setSelected(enabled);
  }

  private void createUIComponents() {
  }


  /**
   * Method generated by IntelliJ IDEA GUI Designer
   * >>> IMPORTANT!! <<<
   * DO NOT edit this method OR call it in your code!
   *
   * @noinspection ALL
   */
  private void $$$setupUI$$$() {
    createUIComponents();
    container = new JPanel();
    container.setLayout(new GridBagLayout());
    storageLocationContainer = new JPanel();
    storageLocationContainer.setLayout(new GridBagLayout());
    GridBagConstraints gbc;
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 1;
    gbc.gridwidth = 5;
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    container.add(storageLocationContainer, gbc);
    storageLocationLabel = new JLabel();
    storageLocationLabel.setText("Storage Location");
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.anchor = GridBagConstraints.WEST;
    storageLocationContainer.add(storageLocationLabel, gbc);
    storageLocationField = new JTextField();
    storageLocationField.setEditable(false);
    gbc = new GridBagConstraints();
    gbc.gridx = 2;
    gbc.gridy = 0;
    gbc.weightx = 1.0;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    storageLocationContainer.add(storageLocationField, gbc);
    storageLocationButtons = new JPanel();
    storageLocationButtons.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
    gbc = new GridBagConstraints();
    gbc.gridx = 3;
    gbc.gridy = 0;
    gbc.fill = GridBagConstraints.BOTH;
    storageLocationContainer.add(storageLocationButtons, gbc);
    storageLocationClearButton = new JButton();
    storageLocationClearButton.setText("Clear");
    storageLocationClearButton.setVisible(false);
    storageLocationButtons.add(storageLocationClearButton);
    storageLocationChooseButton = new JButton();
    storageLocationChooseButton.setText("Choose...");
    storageLocationButtons.add(storageLocationChooseButton);
    final JPanel spacer1 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    storageLocationContainer.add(spacer1, gbc);
    final JPanel spacer2 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 2;
    gbc.gridwidth = 5;
    gbc.fill = GridBagConstraints.VERTICAL;
    container.add(spacer2, gbc);
    pullInParallelField = new JCheckBox();
    pullInParallelField.setText("Pull submissions in parallel (experimental)");
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 3;
    gbc.gridwidth = 5;
    gbc.anchor = GridBagConstraints.WEST;
    container.add(pullInParallelField, gbc);
    rememberPasswordsField = new JCheckBox();
    rememberPasswordsField.setText("Remember passwords (unencrypted)");
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 5;
    gbc.gridwidth = 5;
    gbc.anchor = GridBagConstraints.WEST;
    container.add(rememberPasswordsField, gbc);
    sendUsageDataField = new JCheckBox();
    sendUsageDataField.setText("Send usage data and crash logs to core developers");
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 7;
    gbc.gridwidth = 5;
    gbc.anchor = GridBagConstraints.WEST;
    container.add(sendUsageDataField, gbc);
    useHttpProxyField = new JCheckBox();
    useHttpProxyField.setText("Use HTTP Proxy");
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 9;
    gbc.gridwidth = 5;
    gbc.anchor = GridBagConstraints.WEST;
    container.add(useHttpProxyField, gbc);
    final JPanel spacer3 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 10;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.insets = new Insets(0, 0, 0, 20);
    container.add(spacer3, gbc);
    httpProxyJostLabel = new JLabel();
    httpProxyJostLabel.setText("Host");
    gbc = new GridBagConstraints();
    gbc.gridx = 2;
    gbc.gridy = 10;
    gbc.anchor = GridBagConstraints.WEST;
    container.add(httpProxyJostLabel, gbc);
    httpProxyHostField = new JTextField();
    httpProxyHostField.setPreferredSize(new Dimension(150, 30));
    gbc = new GridBagConstraints();
    gbc.gridx = 4;
    gbc.gridy = 10;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    container.add(httpProxyHostField, gbc);
    final JPanel spacer4 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 3;
    gbc.gridy = 10;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    container.add(spacer4, gbc);
    httpProxyPortLabel = new JLabel();
    httpProxyPortLabel.setText("Port");
    gbc = new GridBagConstraints();
    gbc.gridx = 2;
    gbc.gridy = 11;
    gbc.anchor = GridBagConstraints.WEST;
    container.add(httpProxyPortLabel, gbc);
    final JPanel spacer5 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 5;
    gbc.gridy = 10;
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    container.add(spacer5, gbc);
    httpProxyPortField.setPreferredSize(new Dimension(150, 30));
    gbc = new GridBagConstraints();
    gbc.gridx = 4;
    gbc.gridy = 11;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    container.add(httpProxyPortField, gbc);
    final JPanel spacer6 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 8;
    gbc.gridwidth = 5;
    gbc.fill = GridBagConstraints.VERTICAL;
    container.add(spacer6, gbc);
    final JPanel spacer7 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 6;
    gbc.gridy = 1;
    gbc.gridheight = 11;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    container.add(spacer7, gbc);
    final JPanel spacer8 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 1;
    gbc.gridheight = 11;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    container.add(spacer8, gbc);
    final JPanel spacer9 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 0;
    gbc.gridwidth = 5;
    gbc.fill = GridBagConstraints.VERTICAL;
    container.add(spacer9, gbc);
    final JPanel spacer10 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 12;
    gbc.gridwidth = 5;
    gbc.weighty = 1.0;
    gbc.fill = GridBagConstraints.VERTICAL;
    container.add(spacer10, gbc);
    final JPanel spacer11 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 6;
    gbc.gridwidth = 5;
    gbc.fill = GridBagConstraints.VERTICAL;
    container.add(spacer11, gbc);
    final JPanel spacer12 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 4;
    gbc.gridwidth = 5;
    gbc.fill = GridBagConstraints.VERTICAL;
    container.add(spacer12, gbc);
  }

  /**
   * @noinspection ALL
   */
  public JComponent $$$getRootComponent$$$() {
    return container;
  }
}
