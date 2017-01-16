package asf.modelpreview.desktop;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

/**
 * Created by Danny on 10/30/2014.
 */
public class ComboStringConfigPanel {

        private JComboBox comboBox;

        public ComboStringConfigPanel(final DesktopLauncher desktopLauncher, final String prefsKey, JPanel parentPanel,
                                      String label, final String defaultValue, final String[] options) {


                JPanel basePanel = new JPanel();
                parentPanel.add(basePanel);

                JLabel labelPanel = new JLabel(label);
                basePanel.add(labelPanel);

                comboBox = new JComboBox();
                basePanel.add(comboBox);
                comboBox.setEditable(false);
                String currentValue = desktopLauncher.prefs.get(prefsKey, defaultValue);
                for (String option : options) {
                        comboBox.addItem(option);
                }
                comboBox.setSelectedItem(currentValue);

                onChange();
                comboBox.addItemListener(new ItemListener() {
                        @Override
                        public void itemStateChanged(ItemEvent e) {
                                desktopLauncher.prefs.put(prefsKey, getValue());
                                onChange();
                        }
                });


        }

        public String getValue(){
                return (String)comboBox.getSelectedItem();
        }

        protected void onChange(){

        }

}

