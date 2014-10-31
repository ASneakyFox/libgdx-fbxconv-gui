package asf.modelpreview.desktop;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

/**
 * Created by Danny on 10/30/2014.
 */
public class BooleanConfigPanel {

        protected JCheckBox checkBox;

        public BooleanConfigPanel(final DesktopLauncher desktopLauncher, final String prefsKey, JPanel parentPanel,
                                  String label, final boolean defaultValue) {


                checkBox = new JCheckBox(label);

                parentPanel.add(checkBox);
                boolean currentValue = desktopLauncher.prefs.getBoolean(prefsKey,defaultValue);
                checkBox.setSelected(currentValue);
                onChange();

                checkBox.addItemListener(new ItemListener() {
                        @Override
                        public void itemStateChanged(ItemEvent e) {
                                desktopLauncher.prefs.putBoolean(prefsKey, checkBox.isSelected());
                                onChange();
                        }
                });


        }

        public boolean getValue(){
                return checkBox.isSelected();
        }
        public boolean isSelected(){
                return checkBox.isSelected();
        }

        protected void onChange(){

        }
}

