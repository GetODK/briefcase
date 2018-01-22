package org.opendatakit.briefcase.ui.export.components;

import static java.awt.event.KeyEvent.VK_ESCAPE;
import static javax.swing.JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT;
import static javax.swing.KeyStroke.getKeyStroke;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import org.opendatakit.briefcase.ui.reused.WindowAdapterBuilder;

@SuppressWarnings("checkstyle:MethodName")
public class ConfigurationDialogForm extends JDialog {
  private JPanel dialog;
  protected JButton okButton;
  private JButton cancelButton;
  protected JButton removeButton;
  private JPanel rightActions;
  private JPanel leftActions;
  private JPanel actions;
  private ConfigurationPanelForm configurationPanelForm;

  ConfigurationDialogForm(ConfigurationPanelForm form) {
    configurationPanelForm = form;
    $$$setupUI$$$();
    setContentPane(dialog);
    setModal(true);
    getRootPane().setDefaultButton(okButton);
    pack();
    setLocationRelativeTo(null);
    setTitle("Export configuration");

    okButton.addActionListener(e -> dispose());
    removeButton.addActionListener(e -> dispose());
    cancelButton.addActionListener(e -> dispose());

    setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

    addWindowListener(new WindowAdapterBuilder().onClosing(e -> dispose()).build());

    dialog.registerKeyboardAction(e -> dispose(), getKeyStroke(VK_ESCAPE, 0), WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
  }

  public void open() {
    setVisible(true);
  }

  public void onRemove(Runnable callback) {
    removeButton.addActionListener(__ -> callback.run());
  }

  public void onOK(Runnable callback) {
    okButton.addActionListener(__ -> callback.run());
  }

  public void enableOK() {
    okButton.setEnabled(true);
  }

  public void disableOK() {
    okButton.setEnabled(false);
  }

  public void enableRemove() {
    removeButton.setEnabled(true);
  }

  @Override
  public void setEnabled(boolean enabled) {
    if (enabled) {
      for (Component c : dialog.getComponents())
        c.setEnabled(true);
      dialog.setEnabled(true);
    } else {
      for (Component c : dialog.getComponents())
        c.setEnabled(false);
      dialog.setEnabled(false);
    }
  }

  private void createUIComponents() {
    // Custom creation of components occurs inside the constructor
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
    dialog = new JPanel();
    dialog.setLayout(new GridBagLayout());
    actions = new JPanel();
    actions.setLayout(new GridBagLayout());
    GridBagConstraints gbc;
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 3;
    gbc.weightx = 1.0;
    gbc.anchor = GridBagConstraints.SOUTH;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    dialog.add(actions, gbc);
    rightActions = new JPanel();
    rightActions.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
    gbc = new GridBagConstraints();
    gbc.gridx = 2;
    gbc.gridy = 0;
    gbc.weighty = 1.0;
    gbc.fill = GridBagConstraints.BOTH;
    actions.add(rightActions, gbc);
    removeButton = new JButton();
    removeButton.setEnabled(false);
    removeButton.setText("Remove");
    rightActions.add(removeButton);
    okButton = new JButton();
    okButton.setEnabled(false);
    okButton.setText("OK");
    rightActions.add(okButton);
    leftActions = new JPanel();
    leftActions.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.weighty = 1.0;
    gbc.fill = GridBagConstraints.BOTH;
    actions.add(leftActions, gbc);
    cancelButton = new JButton();
    cancelButton.setText("Cancel");
    leftActions.add(cancelButton);
    final JPanel spacer1 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 0;
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    actions.add(spacer1, gbc);
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 1;
    gbc.anchor = GridBagConstraints.NORTH;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    dialog.add(configurationPanelForm.$$$getRootComponent$$$(), gbc);
    final JPanel spacer2 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 2;
    gbc.weighty = 1.0;
    gbc.fill = GridBagConstraints.VERTICAL;
    dialog.add(spacer2, gbc);
    final JPanel spacer3 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 0;
    gbc.fill = GridBagConstraints.VERTICAL;
    dialog.add(spacer3, gbc);
    final JPanel spacer4 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 4;
    gbc.fill = GridBagConstraints.VERTICAL;
    dialog.add(spacer4, gbc);
    final JPanel spacer5 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 2;
    gbc.gridy = 1;
    gbc.gridheight = 4;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    dialog.add(spacer5, gbc);
    final JPanel spacer6 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 1;
    gbc.gridheight = 4;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    dialog.add(spacer6, gbc);
  }

  /**
   * @noinspection ALL
   */
  public JComponent $$$getRootComponent$$$() {
    return dialog;
  }
}
