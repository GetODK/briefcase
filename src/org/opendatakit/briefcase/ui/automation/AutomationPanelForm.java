package org.opendatakit.briefcase.ui.automation;

import static org.opendatakit.briefcase.ui.reused.FileChooser.directory;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import org.opendatakit.briefcase.automation.AutomationConfiguration;
import org.opendatakit.briefcase.export.ExportConfiguration;
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.reused.http.Http;
import org.opendatakit.briefcase.transfer.TransferForms;
import org.opendatakit.briefcase.ui.export.components.ConfigurationDialog;
import org.opendatakit.briefcase.ui.reused.source.Source;
import org.opendatakit.briefcase.ui.reused.source.SourcePanel;
import org.opendatakit.briefcase.ui.reused.source.SourcePanelForm;
import org.opendatakit.briefcase.util.StringUtils;

@SuppressWarnings("checkstyle:MethodName")
public class AutomationPanelForm {
  public JPanel container;
  private JLabel scriptDirLabel;
  private JTextField scriptDirField;
  private JButton scriptDirChooseButton;
  private JButton generateScriptButton;
  private JButton setExportConfigurationButton;
  private JScrollPane scrollPane;
  private final TransferFormsTable formsTable;
  private TransferFormsTableView formsTableView;
  private SourcePanel pullSourcePanel;
  private SourcePanel pushSourcePanel;
  private SourcePanelForm pullSourcePanelForm;
  private SourcePanelForm pushSourcePanelForm;

  private Optional<ExportConfiguration> exportConfiguration = Optional.empty();

  private List<Runnable> onChandeCallbacks = new ArrayList<>();

  public AutomationPanelForm(SourcePanel pullSourcePanel, SourcePanel pushSourcePanel, TransferFormsTable formsTable) {
    this.formsTable = formsTable;
    this.formsTableView = formsTable.getView();
    this.pullSourcePanel = pullSourcePanel;
    this.pullSourcePanelForm = pullSourcePanel.getContainer();
    this.pushSourcePanel = pushSourcePanel;
    this.pushSourcePanelForm = pushSourcePanel.getContainer();
    $$$setupUI$$$();
    scriptDirChooseButton.addActionListener(__ ->
        directory(container, fileFrom(scriptDirField))
            .choose()
            .ifPresent(file -> setScriptDir(Paths.get(file.toURI()))));
    setExportConfigurationButton.addActionListener(__ -> setExportConfiguration());
    formsTable.onChange(this::triggerOnChange);
  }

  private void setExportConfiguration() {
    ConfigurationDialog dialog = ConfigurationDialog.defaultPanel(exportConfiguration, BriefcasePreferences.getStorePasswordsConsentProperty());
    dialog.onOK(config -> exportConfiguration = Optional.ofNullable(config));
    dialog.open();
  }

  void onGenerate(Consumer<AutomationConfiguration> callback) {
    generateScriptButton.addActionListener(__ -> {
      AutomationConfiguration automationConfiguration = new AutomationConfiguration(
          Optional.ofNullable(Paths.get(scriptDirField.getText())),
          exportConfiguration
      );
      callback.accept(automationConfiguration);
    });
  }

  void onPullSource(Consumer<Source<?>> callback) {
    pullSourcePanel.onSource(callback);
  }

  void onPushSource(Consumer<Source<?>> callback) {
    pushSourcePanel.onSource(callback);
  }

  private void triggerOnChange() {
    onChandeCallbacks.forEach(Runnable::run);
  }

  private void setScriptDir(Path path) {
    scriptDirField.setText(path.toString());
  }

  private static Optional<File> fileFrom(JTextField textField) {
    return Optional.ofNullable(textField.getText())
        .filter(StringUtils::nullOrEmpty)
        .map(path -> Paths.get(path).toFile());
  }

  static AutomationPanelForm from(Http http, TransferForms forms) {
    return new AutomationPanelForm(SourcePanel.pull(http), SourcePanel.push(http), TransferFormsTable.from(forms, "Export"));
  }

  public void refresh() {
    formsTable.refresh();
  }

  public void showConfirmation() {
    JOptionPane.showMessageDialog(container, "Script Generated");
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
    scriptDirLabel = new JLabel();
    scriptDirLabel.setText("Script Location");
    GridBagConstraints gbc;
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 1;
    gbc.anchor = GridBagConstraints.WEST;
    container.add(scriptDirLabel, gbc);
    final JPanel spacer1 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    container.add(spacer1, gbc);
    scriptDirField = new JTextField();
    scriptDirField.setEditable(false);
    gbc = new GridBagConstraints();
    gbc.gridx = 2;
    gbc.gridy = 1;
    gbc.weightx = 1.0;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    container.add(scriptDirField, gbc);
    scriptDirChooseButton = new JButton();
    scriptDirChooseButton.setText("Choose...");
    gbc = new GridBagConstraints();
    gbc.gridx = 4;
    gbc.gridy = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    container.add(scriptDirChooseButton, gbc);
    generateScriptButton = new JButton();
    generateScriptButton.setText("Generate Script");
    gbc = new GridBagConstraints();
    gbc.gridx = 2;
    gbc.gridy = 11;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    container.add(generateScriptButton, gbc);
    final JPanel spacer2 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 2;
    gbc.gridy = 12;
    gbc.weighty = 1.0;
    gbc.fill = GridBagConstraints.VERTICAL;
    container.add(spacer2, gbc);
    final JPanel spacer3 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 2;
    gbc.gridy = 0;
    gbc.fill = GridBagConstraints.VERTICAL;
    container.add(spacer3, gbc);
    setExportConfigurationButton = new JButton();
    setExportConfigurationButton.setText("Set export configuration");
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 7;
    gbc.gridwidth = 5;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    container.add(setExportConfigurationButton, gbc);
    final JPanel spacer4 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 2;
    gbc.gridy = 8;
    gbc.fill = GridBagConstraints.VERTICAL;
    container.add(spacer4, gbc);
    final JPanel spacer5 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 2;
    gbc.gridy = 2;
    gbc.fill = GridBagConstraints.VERTICAL;
    container.add(spacer5, gbc);
    scrollPane = new JScrollPane();
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 9;
    gbc.gridwidth = 5;
    gbc.weightx = 1.0;
    gbc.weighty = 1.0;
    gbc.fill = GridBagConstraints.BOTH;
    container.add(scrollPane, gbc);
    scrollPane.setViewportView(formsTableView);
    final JPanel spacer6 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 2;
    gbc.gridy = 10;
    gbc.fill = GridBagConstraints.VERTICAL;
    container.add(spacer6, gbc);
    gbc = new GridBagConstraints();
    gbc.gridx = 2;
    gbc.gridy = 3;
    container.add(pullSourcePanelForm.$$$getRootComponent$$$(), gbc);
    final JPanel spacer7 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 2;
    gbc.gridy = 4;
    gbc.fill = GridBagConstraints.VERTICAL;
    container.add(spacer7, gbc);
    final JPanel spacer8 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 3;
    gbc.gridy = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    container.add(spacer8, gbc);
    gbc = new GridBagConstraints();
    gbc.gridx = 2;
    gbc.gridy = 5;
    container.add(pushSourcePanelForm.$$$getRootComponent$$$(), gbc);
    final JPanel spacer9 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 2;
    gbc.gridy = 6;
    gbc.fill = GridBagConstraints.VERTICAL;
    container.add(spacer9, gbc);
  }

  /**
   * @noinspection ALL
   */
  public JComponent $$$getRootComponent$$$() {
    return container;
  }
}
