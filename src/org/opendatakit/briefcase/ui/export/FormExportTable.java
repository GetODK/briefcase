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

import static javax.swing.SortOrder.ASCENDING;
import static org.opendatakit.briefcase.ui.export.FormExportTableModel.DETAIL_BUTTON_COL;
import static org.opendatakit.briefcase.ui.export.FormExportTableModel.FORM_NAME_COL;
import static org.opendatakit.briefcase.ui.export.FormExportTableModel.HEADERS;
import static org.opendatakit.briefcase.ui.export.FormExportTableModel.SELECTED_CHECKBOX_COL;
import static org.opendatakit.briefcase.ui.export.FormExportTableModel.buildDetailButton;

import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.util.Collections;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import org.bushe.swing.event.annotation.AnnotationProcessor;

class FormExportTable extends JTable {
  private static final long serialVersionUID = 8511088963758308085L;

  FormExportTable(FormExportTableModel tableModel) {
    super(tableModel);
    AnnotationProcessor.process(this);

    addMouseListener(new MouseListenerBuilder().onClick(this::relayClickToButton).build());

    Dimension checkboxDims = getTableHeader()
        .getDefaultRenderer()
        .getTableCellRendererComponent(null, HEADERS[SELECTED_CHECKBOX_COL], false, false, 0, 0)
        .getPreferredSize();
    Dimension buttonDims = buildDetailButton(null).getPreferredSize();

    setRowHeight(buttonDims.height);

    TableColumnModel columns = getColumnModel();
    columns.getColumn(SELECTED_CHECKBOX_COL).setMinWidth(checkboxDims.width);
    columns.getColumn(SELECTED_CHECKBOX_COL).setMaxWidth(checkboxDims.width);
    columns.getColumn(SELECTED_CHECKBOX_COL).setPreferredWidth(checkboxDims.width);
    columns.getColumn(DETAIL_BUTTON_COL).setCellRenderer(cellWithButton());
    columns.getColumn(DETAIL_BUTTON_COL).setMinWidth(buttonDims.width);
    columns.getColumn(DETAIL_BUTTON_COL).setMaxWidth(buttonDims.width);
    columns.getColumn(DETAIL_BUTTON_COL).setPreferredWidth(buttonDims.width);

    setFillsViewportHeight(true);

    TableRowSorter<FormExportTableModel> sorter = sortBy((FormExportTableModel) this.dataModel, FORM_NAME_COL, ASCENDING);
    setRowSorter(sorter);
    sorter.sort();
  }

  private void relayClickToButton(MouseEvent event) {
    int column = getColumnModel().getColumnIndexAtX(event.getX());
    int row = event.getY() / getRowHeight();

    if (row < getRowCount() && row >= 0 && column < getColumnCount() && column >= 0) {
      Object value = getValueAt(row, column);
      if (value instanceof JButton)
        ((JButton) value).doClick();
    }
  }

  private static TableCellRenderer cellWithButton() {
    return (table, value, isSelected, hasFocus, row, column) -> {
      JButton button = (JButton) value;
      button.setOpaque(true);
      button.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
      return button;
    };
  }

  private static <T extends TableModel> TableRowSorter<T> sortBy(T model, int col, SortOrder order) {
    TableRowSorter<T> sorter = new TableRowSorter<>(model);
    sorter.setSortsOnUpdates(true);
    sorter.setSortKeys(Collections.singletonList(new RowSorter.SortKey(col, order)));
    return sorter;
  }
}
