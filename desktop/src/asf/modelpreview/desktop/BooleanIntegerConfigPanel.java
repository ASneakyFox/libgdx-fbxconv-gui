package asf.modelpreview.desktop;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

/**
 * Created by Danny on 10/30/2014.
 */
public class BooleanIntegerConfigPanel {

        private final SpinnerNumberModel spinnerModel;
        protected JCheckBox checkBox;

        public BooleanIntegerConfigPanel(final DesktopLauncher desktopLauncher,
                                         JPanel parentPanel,
                                         String label,
                                         final String booleanPrefsKey,
                                         final boolean defaultBooleanValue,
                                         final String prefsKey,
                                         final int defaultValue, int min, int max, int step) {

                JPanel basePanel = new JPanel();
                parentPanel.add(basePanel);

                checkBox = new JCheckBox(label);
                basePanel.add(checkBox);
                boolean currentBooleanValue = desktopLauncher.prefs.getBoolean(booleanPrefsKey, defaultBooleanValue);
                checkBox.setSelected(currentBooleanValue);


                final JSpinner spinner = new JSpinner();
                spinnerModel = new SpinnerNumberModel(defaultValue, min,max, step);
                int currentValue = desktopLauncher.prefs.getInt(prefsKey,defaultValue);
                spinner.setValue(currentValue);
                spinner.setModel(spinnerModel);
                basePanel.add(spinner);
                spinner.setEnabled(checkBox.isSelected());

                onChange();

                spinner.addChangeListener(new ChangeListener() {
                        @Override
                        public void stateChanged(ChangeEvent e) {
                                desktopLauncher.prefs.putInt(prefsKey, spinnerModel.getNumber().intValue());
                                onChange();
                        }
                });

                checkBox.addItemListener(new ItemListener() {
                        @Override
                        public void itemStateChanged(ItemEvent e) {
                                desktopLauncher.prefs.putBoolean(booleanPrefsKey, checkBox.isSelected());
                                spinner.setEnabled(checkBox.isSelected());
                                onChange();
                        }
                });




        }

        protected void onChange(){

        }

        public int getIntegerValue(){
                return spinnerModel.getNumber().intValue();
        }

        public boolean getBooleanValue(){
                return checkBox.isSelected();
        }


}
