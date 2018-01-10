package org.opendatakit.briefcase.ui.export;

import static java.awt.Color.DARK_GRAY;
import static java.awt.Color.GREEN;
import static javax.swing.JOptionPane.getFrameForComponent;
import static org.opendatakit.briefcase.ui.ScrollingStatusListDialog.showDialog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.swing.JButton;
import javax.swing.table.AbstractTableModel;
import org.bushe.swing.event.annotation.AnnotationProcessor;
import org.bushe.swing.event.annotation.EventSubscriber;
import org.opendatakit.briefcase.model.BriefcaseFormDefinition;
import org.opendatakit.briefcase.model.ExportFailedEvent;
import org.opendatakit.briefcase.model.ExportProgressEvent;
import org.opendatakit.briefcase.model.ExportSucceededEvent;
import org.opendatakit.briefcase.model.ExportSucceededWithErrorsEvent;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.model.FormStatusEvent;

class FormExportTableModel extends AbstractTableModel {
  private static final long serialVersionUID = 7108326237416622721L;
  static final String[] HEADERS = new String[]{"Selected", "⚙", "Form Name", "Export Status", "Detail"};

  static final int SELECTED_CHECKBOX_COL = 0;
  static final int OVERRIDE_CONF_COL = 1;
  static final int FORM_NAME_COL = 2;
  private static final int EXPORT_STATUS_COL = 3;
  static final int DETAIL_BUTTON_COL = 4;

  private final List<Runnable> selectionChangeCallbacks = new ArrayList<>();
  private List<FormStatus> forms = new ArrayList<>();
  private Map<FormStatus, JButton> detailButtons = new HashMap<>();
  private Map<FormStatus, JButton> confButtons = new HashMap<>();
  private Map<FormStatus, ExportConfiguration> confs = new HashMap<>();

  FormExportTableModel() {
    super();
    AnnotationProcessor.process(this);
  }

  void setForms(List<FormStatus> forms) {
    this.forms = forms;
    fireTableDataChanged();
  }

  List<FormStatus> getSelectedForms() {
    return forms.stream().filter(FormStatus::isSelected).collect(Collectors.toList());
  }

  boolean noneSelected() {
    return forms.stream().noneMatch(FormStatus::isSelected);
  }

  boolean allSelected() {
    return forms.stream().allMatch(FormStatus::isSelected);
  }

  void checkAll() {
    for (int row = 0; row < forms.size(); row++)
      setValueAt(true, row, 0);
  }

  void uncheckAll() {
    for (int row = 0; row < forms.size(); row++)
      setValueAt(false, row, 0);
  }

  void onSelectionChange(Runnable callback) {
    selectionChangeCallbacks.add(callback);
  }

  private void selectionChange() {
    selectionChangeCallbacks.forEach(Runnable::run);
  }

  private Optional<Integer> findRow(FormStatus formInEvent) {
    return Optional.of(forms.indexOf(formInEvent)).filter(i -> i != -1);
  }

  private Optional<Integer> findRow(BriefcaseFormDefinition formDefinition) {
    for (int index = 0; index < forms.size(); index++)
      if (forms.get(index).getFormDefinition() == formDefinition)
        return Optional.of(index);
    return Optional.empty();
  }

  JButton buildDetailButton(FormStatus form) {
    JButton button = new JButton("Details...");
    // Ugly hack to be able to use this factory in FormExportTable to compute its Dimension
    if (form != null) {
      button.addActionListener(__ -> {
        button.setEnabled(false);
        try {
          showDialog(getFrameForComponent(button), form.getFormDefinition(), form.getStatusHistory());
        } finally {
          button.setEnabled(true);
        }
      });
    }
    return button;
  }

  JButton buildOverrideConfButton(FormStatus form) {
    JButton button = new JButton("⚙");
    if (confs.containsKey(form))
      button.setForeground(GREEN);
    // Ugly hack to be able to use this factory in FormExportTable to compute its Dimension
    if (form != null) {
      button.addActionListener(__ -> {
        button.setEnabled(false);
        try {
          ExportConfigurationDialog dialog = ExportConfigurationDialog.from(
              getFrameForComponent(button),
              confs.computeIfAbsent(form, ___ -> ExportConfiguration.empty()),
              () -> {
                confs.remove(form);
                button.setForeground(DARK_GRAY);
              },
              config -> {
                confs.put(form, config);
                button.setForeground(GREEN);
              }
          );
          dialog.open();
        } finally {
          button.setEnabled(true);
        }
      });
    }
    return button;
  }

  Optional<ExportConfiguration> getConf(BriefcaseFormDefinition formDefinition) {
    return confs.keySet().stream()
        .filter(form -> form.getFormDefinition().equals(formDefinition))
        .findFirst()
        .map(confs::get);
  }

  @Override
  public int getRowCount() {
    return forms.size();
  }

  @Override
  public String getColumnName(int col) {
    return HEADERS[col];
  }

  @Override
  public int getColumnCount() {
    return HEADERS.length;
  }


  @SuppressWarnings({"rawtypes", "unchecked"})
  @Override
  public Class getColumnClass(int col) {
    switch (col) {
      case SELECTED_CHECKBOX_COL:
        return Boolean.class;
      case OVERRIDE_CONF_COL:
        return JButton.class;
      case FORM_NAME_COL:
        return String.class;
      case EXPORT_STATUS_COL:
        return String.class;
      case DETAIL_BUTTON_COL:
        return JButton.class;
      default:
        throw new IllegalStateException("unexpected column choice");
    }
  }

  @Override
  public boolean isCellEditable(int row, int col) {
    return col == SELECTED_CHECKBOX_COL;
  }

  @Override
  public void setValueAt(Object value, int row, int col) {
    FormStatus form = forms.get(row);
    switch (col) {
      case SELECTED_CHECKBOX_COL:
        form.setSelected((Boolean) value);
        selectionChange();
        break;
      case EXPORT_STATUS_COL:
        form.setStatusString((String) value, true);
        break;
      default:
        throw new IllegalStateException("unexpected column choice");
    }
    fireTableCellUpdated(row, col);
  }

  @Override
  public Object getValueAt(int row, int col) {
    FormStatus form = forms.get(row);
    switch (col) {
      case SELECTED_CHECKBOX_COL:
        return form.isSelected();
      case OVERRIDE_CONF_COL:
        return confButtons.computeIfAbsent(form, this::buildOverrideConfButton);
      case FORM_NAME_COL:
        return form.getFormName();
      case EXPORT_STATUS_COL:
        return form.getStatusString();
      case DETAIL_BUTTON_COL:
        return detailButtons.computeIfAbsent(form, this::buildDetailButton);
      default:
        throw new IllegalStateException("unexpected column choice");
    }
  }

  @EventSubscriber(eventClass = FormStatusEvent.class)
  public void onFormStatusEvent(FormStatusEvent event) {
    findRow(event.getStatus()).ifPresent(row -> {
      forms.get(row).setStatusString(event.getStatusString(), false);
      fireTableRowsUpdated(row, row);
    });
  }

  @EventSubscriber(eventClass = ExportProgressEvent.class)
  public void onExportProgressEvent(ExportProgressEvent event) {
    findRow(event.getFormDefinition()).ifPresent(row -> {
      forms.get(row).setStatusString(event.getText(), false);
      fireTableRowsUpdated(row, row);
    });
  }

  @EventSubscriber(eventClass = ExportFailedEvent.class)
  public void onExportFailedEvent(ExportFailedEvent event) {
    findRow(event.getFormDefinition()).ifPresent(row -> {
      forms.get(row).setStatusString("Failed.", false);
      fireTableRowsUpdated(row, row);
    });
  }

  @EventSubscriber(eventClass = ExportSucceededEvent.class)
  public void onExportSucceededEvent(ExportSucceededEvent event) {
    findRow(event.getFormDefinition()).ifPresent(row -> {
      forms.get(row).setStatusString("Succeeded.", true);
      fireTableRowsUpdated(row, row);
    });
  }

  @EventSubscriber(eventClass = ExportSucceededWithErrorsEvent.class)
  public void onExportSucceededWithErrorsEvent(ExportSucceededWithErrorsEvent event) {
    findRow(event.getFormDefinition()).ifPresent(row -> {
      forms.get(row).setStatusString("Succeeded, but with errors.", true);
      fireTableRowsUpdated(row, row);
    });
  }

}
