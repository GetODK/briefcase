/*
 * Copyright (C) 2011 University of Washington.
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

package org.opendatakit.briefcase.ui.export;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static javax.swing.GroupLayout.Alignment.BASELINE;
import static javax.swing.GroupLayout.Alignment.LEADING;
import static javax.swing.LayoutStyle.ComponentPlacement.RELATED;
import static javax.swing.LayoutStyle.ComponentPlacement.UNRELATED;
import static org.opendatakit.briefcase.model.FormStatus.TransferType.EXPORT;
import static org.opendatakit.briefcase.ui.ODKOptionPane.showErrorDialog;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import org.bushe.swing.event.annotation.AnnotationProcessor;
import org.bushe.swing.event.annotation.EventSubscriber;
import org.opendatakit.briefcase.model.BriefcaseFormDefinition;
import org.opendatakit.briefcase.model.ExportType;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.model.TerminationFuture;
import org.opendatakit.briefcase.model.TransferSucceededEvent;
import org.opendatakit.briefcase.ui.export.components.ConfigurationPanel;
import org.opendatakit.briefcase.ui.export.components.FormExportTable;
import org.opendatakit.briefcase.ui.export.components.FormExportTableModel;
import org.opendatakit.briefcase.ui.reused.MouseListenerBuilder;
import org.opendatakit.briefcase.util.ExportAction;
import org.opendatakit.briefcase.util.FileSystemUtils;

public class ExportPanel extends JPanel {

  private static final long serialVersionUID = 7169316129011796197L;

  public static final String TAB_NAME = "Export";
  private final FormExportTableModel tableModel;

  private final JButton btnSelectAll;
  private final JButton btnClearAll;
  private final JButton btnExport;

  private final ConfigurationPanel defaultConfiguration;

  private boolean exportStateActive = false;

  private final TerminationFuture terminationFuture;

  public ExportPanel(TerminationFuture terminationFuture) {
    super();
    AnnotationProcessor.process(this);// if not using AOP

    this.terminationFuture = terminationFuture;

    tableModel = new FormExportTableModel();
    tableModel.onChange(() -> {
      this.updateSelectAllButton();
      this.updateClearAllButton();
      this.updateExportButton();
    });

    btnSelectAll = new JButton("Select all");
    btnSelectAll.addMouseListener(new MouseListenerBuilder().onClick(__ -> tableModel.checkAll()).build());

    btnClearAll = new JButton("Clear all");
    btnClearAll.setVisible(false);
    btnClearAll.addMouseListener(new MouseListenerBuilder().onClick(__ -> tableModel.uncheckAll()).build());

    JLabel lblFormsToTransfer = new JLabel("Forms to export:");

    JScrollPane scrollPane = new JScrollPane(new FormExportTable(tableModel));
    JSeparator separatorFormsList = new JSeparator();

    btnExport = new JButton("Export");
    disableExportButton();
    btnExport.addActionListener(__ -> new Thread(() -> {
      disableExportButton();
      List<String> errors = export();
      if (!errors.isEmpty()) {
        String message = String.format("%s\n\n%s", "We have found some errors while performing the requested export actions:", errors.stream().map(e -> "- " + e).collect(joining("\n")));
        showErrorDialog(this, message, "Export error report");
      }
      enableExportButton();
    }).start());

    defaultConfiguration = ConfigurationPanel.from(ExportConfiguration.empty());
    defaultConfiguration.onChange(this::updateExportButton);

    JPanel bottomPane = new JPanel();
    GroupLayout bottomLayout = new GroupLayout(bottomPane);
    bottomLayout.setHorizontalGroup(bottomLayout.createSequentialGroup()
        .addGroup(bottomLayout.createSequentialGroup()
            .addComponent(btnSelectAll)
            .addComponent(btnClearAll)
            .addPreferredGap(UNRELATED)
            .addComponent(btnExport)
        ));
    bottomLayout.setVerticalGroup(bottomLayout.createParallelGroup(BASELINE)
        .addComponent(btnExport)
        .addComponent(btnSelectAll)
        .addComponent(btnClearAll)
    );
    bottomPane.setLayout(bottomLayout);


    GroupLayout panelLayout = new GroupLayout(this);

    panelLayout.setHorizontalGroup(panelLayout.createSequentialGroup()
        .addContainerGap()
        .addGroup(panelLayout.createParallelGroup(LEADING)
            .addComponent(defaultConfiguration.getView())
            .addComponent(separatorFormsList)
            .addComponent(lblFormsToTransfer)
            .addComponent(scrollPane)
            .addComponent(bottomPane)
        )
        .addContainerGap());

    panelLayout.setVerticalGroup(panelLayout.createParallelGroup(LEADING)
        .addGroup(panelLayout.createSequentialGroup()
            .addContainerGap()
            .addComponent(defaultConfiguration.getView())
            .addPreferredGap(RELATED)
            .addComponent(separatorFormsList)
            .addPreferredGap(RELATED)
            .addComponent(lblFormsToTransfer)
            .addPreferredGap(RELATED)
            .addComponent(scrollPane)
            .addPreferredGap(RELATED)
            .addComponent(bottomPane)
            .addContainerGap()
        ));

    setLayout(panelLayout);
    updateForms();
    setActiveExportState(exportStateActive);
  }

  private void disableExportButton() {
    btnExport.setEnabled(false);
  }

  private void enableExportButton() {
    btnExport.setEnabled(true);
  }

  private void updateExportButton() {
    if (tableModel.someSelected() && (defaultConfiguration.isValid() || tableModel.allSelectedFormsHaveConfiguration()))
      btnExport.setEnabled(true);
    else
      btnExport.setEnabled(false);

  }

  private void updateClearAllButton() {
    btnClearAll.setVisible(tableModel.allSelected());
  }

  private void updateSelectAllButton() {
    btnSelectAll.setVisible(!tableModel.allSelected());
  }

  public void updateForms() {
    tableModel.setForms(FileSystemUtils.getBriefcaseFormList().stream()
        .map(formDefinition -> new FormStatus(EXPORT, formDefinition))
        .collect(toList()));
  }

  @Override
  public void setEnabled(boolean enabled) {
    super.setEnabled(enabled);
    updateForms();

    for (Component c : this.getComponents()) {
      c.setEnabled(enabled);
    }
    if (enabled) {
      // and then update the widgets based upon the transfer state
      setActiveExportState(exportStateActive);
    }
  }

  private void setActiveExportState(boolean active) {
    if (active) {
      defaultConfiguration.disable();
      disableExportButton();
      terminationFuture.reset();
    } else {
      defaultConfiguration.enable();
      enableExportButton();
      updateExportButton();
    }
    exportStateActive = active;
  }

  private List<String> export() {
    return tableModel.getSelectedForms().parallelStream()
        .peek(FormStatus::clearStatusHistory)
        .map(formStatus -> (BriefcaseFormDefinition) formStatus.getFormDefinition())
        .flatMap(formDefinition -> this.export(formDefinition).stream())
        .collect(toList());
  }

  private List<String> export(BriefcaseFormDefinition formDefinition) {
    List<String> errors = new ArrayList<>();
    ExportConfiguration conf = tableModel.getConf(formDefinition).orElse(defaultConfiguration.getConfiguration());
    Optional<File> pemFile = conf.mapPemFile(Path::toFile).filter(File::exists);
    if ((formDefinition.isFileEncryptedForm() || formDefinition.isFieldEncryptedForm()) && !pemFile.isPresent())
      errors.add(formDefinition.getFormName() + " form requires is encrypted and you haven't defined a valid private key file location");
    else
      try {
        ExportAction.export(
            conf.mapExportDir(Path::toFile).orElseThrow(() -> new RuntimeException("Wrong export configuration")),
            ExportType.CSV,
            formDefinition,
            pemFile.orElse(null),
            terminationFuture,
            conf.mapDateRangeStart((LocalDate ld) -> Date.from(ld.atStartOfDay(ZoneId.systemDefault()).toInstant())).orElse(null),
            conf.mapDateRangeEnd((LocalDate ld) -> Date.from(ld.atStartOfDay(ZoneId.systemDefault()).toInstant())).orElse(null)
        );
      } catch (IOException ex) {
        errors.add("Export of form " + formDefinition.getFormName() + " has failed: " + ex.getMessage());
      }
    return errors;
  }

  @EventSubscriber(eventClass = TransferSucceededEvent.class)
  public void onTransferSucceededEvent(TransferSucceededEvent event) {
    updateForms();
  }
}
