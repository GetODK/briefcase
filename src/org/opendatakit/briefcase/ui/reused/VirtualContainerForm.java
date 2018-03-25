package org.opendatakit.briefcase.ui.reused;

import java.awt.CardLayout;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JComponent;
import javax.swing.JPanel;

@SuppressWarnings("checkstyle:MethodName")
public class VirtualContainerForm extends JComponent {
  public JPanel container;
  private Map<String, JPanel> forms = new HashMap<>();

  public VirtualContainerForm() {
    $$$setupUI$$$();
  }

  public void addForm(String key, JPanel form) {
    forms.put(key, form);
    container.add(form);
  }

  public void navigateTo(String key) {
    if (!forms.containsKey(key))
      throw new IllegalArgumentException("Navigation error : key " + key + " not found");

    forms.forEach((s, jpanel) -> jpanel.setVisible(false));
    forms.get(key).setVisible(true);
  }

  /**
   * Method generated by IntelliJ IDEA GUI Designer
   * >>> IMPORTANT!! <<<
   * DO NOT edit this method OR call it in your code!
   *
   * @noinspection ALL
   */
  private void $$$setupUI$$$() {
    container = new JPanel();
    container.setLayout(new CardLayout(0, 0));
  }

  /**
   * @noinspection ALL
   */
  public JComponent $$$getRootComponent$$$() {
    return container;
  }
}
