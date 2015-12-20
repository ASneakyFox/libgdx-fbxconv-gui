package asf.modelpreview.desktop;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.peer.DesktopPeer;

/**
 * Created by Danny on 10/30/2014.
 */
public class NumberConfigPanel {

        private final SpinnerNumberModel spinnerModel;

        public NumberConfigPanel(final DesktopLauncher desktopLauncher, final String prefsKey,JPanel parentPanel,
                                 String label, final int defaultValue, int min, int max, int step) {

                JPanel basePanel = new JPanel();
                parentPanel.add(basePanel);

                JLabel labelPanel = new JLabel(label);
                basePanel.add(labelPanel);

                JSpinner spinner = new JSpinner();
                spinnerModel = new SpinnerNumberModel(defaultValue, min,max, step);
                int currentValue = desktopLauncher.prefs.getInt(prefsKey,defaultValue);
                spinner.setValue(currentValue);
                spinner.setModel(spinnerModel);
		onChange();
                spinner.addChangeListener(new ChangeListener() {
                        @Override
                        public void stateChanged(ChangeEvent e) {
                                desktopLauncher.prefs.putInt(prefsKey, spinnerModel.getNumber().intValue());
				onChange();
                        }
                });
                basePanel.add(spinner);

                JButton resetButton = new JButton("Reset To Default");
                resetButton.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                                spinnerModel.setValue(defaultValue);
                        }
                });
                basePanel.add(resetButton);

        }

        public int getValue(){
                return spinnerModel.getNumber().intValue();
        }
        public String getString(){
                return String.valueOf(getValue());
        }

        protected void onChange(){

        }
}
