package org.opendatakit.briefcase.ui.export;

import static org.opendatakit.briefcase.ui.export.FormExportTableModel.SELECTED_CHECKBOX_COL;

import javax.swing.JFrame;
import org.assertj.swing.core.MouseButton;
import org.assertj.swing.core.Robot;
import org.assertj.swing.data.TableCell;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JButtonFixture;
import org.opendatakit.briefcase.model.TerminationFuture;

class ExportPanelPageObject {
  private final ExportPanel exportPanel;
  private FrameFixture window;

  private ExportPanelPageObject(ExportPanel exportPanel, FrameFixture window) {
    this.exportPanel = exportPanel;
    this.window = window;
  }

  static ExportPanelPageObject setUp(Robot robot) {
    ExportPanel exportPanel = GuiActionRunner.execute(() -> new ExportPanel(new TerminationFuture()));
    JFrame testFrame = GuiActionRunner.execute(() -> {
      JFrame f = new JFrame();
      f.add(exportPanel);
      return f;
    });
    FrameFixture window = new FrameFixture(robot, testFrame);
    return new ExportPanelPageObject(exportPanel, window);
  }

  void show() {
    window.show();
  }

  void setExportDirectory(String value) {
    GuiActionRunner.execute(() -> exportPanel.txtExportDirectory.setText(value));
  }

  void selectFormATRow(int row) {
    TableCell cell = TableCell.row(row).column(SELECTED_CHECKBOX_COL);
    if (window.table("forms").cell(cell).value().equals("false"))
      window.table("forms").click(cell, MouseButton.LEFT_BUTTON);
  }

  JButtonFixture exportButton() {
    return window.button("export");
  }
}
