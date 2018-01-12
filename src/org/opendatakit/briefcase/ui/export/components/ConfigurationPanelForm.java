package org.opendatakit.briefcase.ui.export.components;

import static org.opendatakit.briefcase.ui.ODKOptionPane.showErrorDialog;
import static org.opendatakit.briefcase.ui.StorageLocation.isUnderBriefcaseFolder;
import static org.opendatakit.briefcase.ui.reused.FileChooser.directory;
import static org.opendatakit.briefcase.ui.reused.FileChooser.file;
import static org.opendatakit.briefcase.util.FileSystemUtils.isUnderODKFolder;

import com.github.lgooddatepicker.components.DatePicker;
import com.github.lgooddatepicker.zinternaltools.DateChangeEvent;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import org.opendatakit.briefcase.ui.reused.FileChooser;
import org.opendatakit.briefcase.util.StringUtils;

public class ConfigurationPanelForm extends JComponent {
  protected JTextField exportDirField;
  protected JTextField pemFileField;
  protected DatePicker startDateField;
  protected DatePicker endDateField;
  private JButton exportDirButton;
  private JButton pemFileButton;
  private JLabel exportDirLabel;
  private JLabel pemFileLabel;
  private JLabel startDateLabel;
  private JLabel endDateLabel;
  public JPanel container;
  private final List<Consumer<Path>> onSelectExportDirCallbacks = new ArrayList<>();
  private final List<Consumer<Path>> onSelectPemFileCallbacks = new ArrayList<>();
  private final List<Consumer<LocalDate>> onSelectStartDateCallbacks = new ArrayList<>();
  private final List<Consumer<LocalDate>> onSelectEndDateCallbacks = new ArrayList<>();

  public ConfigurationPanelForm() {
    startDateField = createDatePicker();
    endDateField = createDatePicker();
    $$$setupUI$$$();
    exportDirButton.addActionListener(event -> buildExportDirDialog().choose()
        .ifPresent(file -> setExportDir(Paths.get(file.toURI()))));
    pemFileButton.addActionListener(__ -> buildPemFileDialog().choose()
        .ifPresent(file -> setPemFile(Paths.get(file.toURI()))));
    startDateField.addDateChangeListener(event -> {
      if (!isDateRangeValid()) {
        showError("Invalid date range: \"From\" date must be before \"To\" date.", "Export configuration error");
        startDateField.clear();
      }
      onSelectStartDateCallbacks.forEach(consumer -> consumer.accept(extractDate(event)));
    });
    endDateField.addDateChangeListener(event -> {
      if (!isDateRangeValid()) {
        showError("Invalid date range: \"From\" date must be before \"To\" date.", "Export configuration error");
        endDateField.clear();
      }
      onSelectEndDateCallbacks.forEach(consumer -> consumer.accept(extractDate(event)));
    });
  }

  private boolean isDateRangeValid() {
    org.threeten.bp.LocalDate startDate = startDateField.getDate();
    org.threeten.bp.LocalDate endDate = endDateField.getDate();
    return startDate == null || endDate == null || startDate.isBefore(endDate);
  }

  @Override
  public void setEnabled(boolean enabled) {
    if (enabled) {
      for (Component c : container.getComponents())
        c.setEnabled(true);
      container.setEnabled(true);
    } else {
      for (Component c : container.getComponents())
        c.setEnabled(false);
      container.setEnabled(false);
    }
  }

  void setExportDir(Path path) {
    exportDirField.setText(path.toString());
    onSelectExportDirCallbacks.forEach(consumer -> consumer.accept(path));
  }

  void setPemFile(Path path) {
    pemFileField.setText(path.toString());
    onSelectPemFileCallbacks.forEach(consumer -> consumer.accept(path));
  }

  void setStartDate(LocalDate date) {
    // Route the change through the date picker's date to avoid repeated set calls
    startDateField.setDate(org.threeten.bp.LocalDate.of(date.getYear(), date.getMonthValue(), date.getDayOfMonth()));
  }

  void setEndDate(LocalDate date) {
    // Route the change through the date picker's date to avoid repeated set calls
    endDateField.setDate(org.threeten.bp.LocalDate.of(date.getYear(), date.getMonthValue(), date.getDayOfMonth()));
  }

  void clearStartDate() {
    startDateField.clear();
  }

  void clearEndDate() {
    endDateField.clear();
  }

  void onSelectExportDir(Consumer<Path> callback) {
    onSelectExportDirCallbacks.add(callback);
  }

  void onSelectPemFile(Consumer<Path> callback) {
    onSelectPemFileCallbacks.add(callback);
  }

  void onSelectDateRangeStart(Consumer<LocalDate> callback) {
    onSelectStartDateCallbacks.add(callback);
  }

  void onSelectDateRangeEnd(Consumer<LocalDate> callback) {
    onSelectEndDateCallbacks.add(callback);
  }

  public void showError(String message, String title) {
    showErrorDialog(container, message, title);
  }

  private void createUIComponents() {
    // Custom creation of components occurs inside the constructor
  }

  /**
   * The DatePicker default text box and calendar button don't match with the rest of the UI.
   * This tweaks those elements to be consistent with the rest of the application.
   */
  private DatePicker createDatePicker() {
    JTextField model = new JTextField();

    DatePicker datePicker = new DatePicker();
    datePicker.getComponentDateTextField().setBorder(model.getBorder());
    datePicker.getComponentDateTextField().setMargin(new Insets(0, 0, 0, 0));
    datePicker.getComponentDateTextField().setEditable(false);
    datePicker.getComponentToggleCalendarButton().setText("Choose...");
    datePicker.getComponentToggleCalendarButton().setMargin(new Insets(0, 0, 0, 0));

    return datePicker;
  }

  private FileChooser buildPemFileDialog() {
    return file(container, fileFrom(pemFileField));
  }

  private FileChooser buildExportDirDialog() {
    return directory(
        container,
        fileFrom(exportDirField),
        f -> f.exists() && f.isDirectory() && !isUnderBriefcaseFolder(f) && !isUnderODKFolder(f),
        "Exclude Briefcase & ODK directories"
    );
  }

  private static Optional<File> fileFrom(JTextField textField) {
    return Optional.ofNullable(textField.getText())
        .filter(StringUtils::nullOrEmpty)
        .map(path -> Paths.get(path).toFile());
  }

  private static LocalDate extractDate(DateChangeEvent event) {
    return event.getNewDate() != null ? LocalDate.of(
        event.getNewDate().getYear(),
        event.getNewDate().getMonthValue(),
        event.getNewDate().getDayOfMonth()
    ) : null;
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
    exportDirLabel = new JLabel();
    exportDirLabel.setText("Export directory");
    GridBagConstraints gbc;
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.anchor = GridBagConstraints.EAST;
    container.add(exportDirLabel, gbc);
    pemFileLabel = new JLabel();
    pemFileLabel.setText("PEM file location");
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 1;
    gbc.anchor = GridBagConstraints.EAST;
    container.add(pemFileLabel, gbc);
    startDateLabel = new JLabel();
    startDateLabel.setText("Start date");
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 2;
    gbc.anchor = GridBagConstraints.EAST;
    container.add(startDateLabel, gbc);
    endDateLabel = new JLabel();
    endDateLabel.setText("End date");
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 3;
    gbc.anchor = GridBagConstraints.EAST;
    container.add(endDateLabel, gbc);
    exportDirField = new JTextField();
    exportDirField.setEditable(false);
    gbc = new GridBagConstraints();
    gbc.gridx = 2;
    gbc.gridy = 0;
    gbc.weightx = 1.0;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    container.add(exportDirField, gbc);
    pemFileField = new JTextField();
    pemFileField.setEditable(false);
    gbc = new GridBagConstraints();
    gbc.gridx = 2;
    gbc.gridy = 1;
    gbc.weightx = 1.0;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    container.add(pemFileField, gbc);
    gbc = new GridBagConstraints();
    gbc.gridx = 2;
    gbc.gridy = 2;
    gbc.gridwidth = 2;
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    container.add(startDateField, gbc);
    gbc = new GridBagConstraints();
    gbc.gridx = 2;
    gbc.gridy = 3;
    gbc.gridwidth = 2;
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    container.add(endDateField, gbc);
    exportDirButton = new JButton();
    exportDirButton.setText("Choose...");
    gbc = new GridBagConstraints();
    gbc.gridx = 3;
    gbc.gridy = 0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    container.add(exportDirButton, gbc);
    pemFileButton = new JButton();
    pemFileButton.setText("Choose...");
    gbc = new GridBagConstraints();
    gbc.gridx = 3;
    gbc.gridy = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    container.add(pemFileButton, gbc);
    final JPanel spacer1 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    container.add(spacer1, gbc);
    final JPanel spacer2 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    container.add(spacer2, gbc);
    final JPanel spacer3 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 2;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    container.add(spacer3, gbc);
    final JPanel spacer4 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 3;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    container.add(spacer4, gbc);
  }

  /**
   * @noinspection ALL
   */
  public JComponent $$$getRootComponent$$$() {
    return container;
  }
}
