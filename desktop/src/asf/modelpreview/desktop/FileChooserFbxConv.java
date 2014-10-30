package asf.modelpreview.desktop;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.filechooser.FileFilter;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.IOException;

/**
 * Created by Danny on 10/30/2014.
 */
public class FileChooserFbxConv {

        private String fbxConvLocation;
        private String fbxConvName;
        /**
         * the filename of the fbx-conv program that should be looked for (depending on the OS)
         */
        private final String fbxConvExecName;
        protected final String dirSeperator;

        public FileChooserFbxConv(final DesktopLauncher desktopLauncher, final String prefsKey, JPanel parentPanel) {

                String osName = System.getProperty("os.name").toLowerCase();
                if(osName.contains("win")){
                        fbxConvExecName = "fbx-conv-win32.exe";
                        dirSeperator="\\";
                }else if(osName.contains("mac")){
                        fbxConvExecName = "fbx-conv-mac";
                        dirSeperator ="/";
                }else{
                        fbxConvExecName = "fbx-conv-lin64";
                        dirSeperator ="/";
                }

                JPanel fbxConvToolBrowsePanel = new JPanel();
                parentPanel.add(fbxConvToolBrowsePanel);

                final JTextField fbxConvLocationField = new JTextField();
                fbxConvToolBrowsePanel.add(fbxConvLocationField);
                fbxConvLocation = desktopLauncher.prefs.get(prefsKey, null);
                File fbxConvFile = new File(fbxConvLocation);
                if(fbxConvLocation != null && !isValidFbxConvFileLocation(fbxConvFile)){
                        fbxConvLocation = null;
                }else{
                        fbxConvLocation = fbxConvFile.getAbsolutePath();
                }
                fbxConvLocationField.setText(fbxConvLocation!= null ? fbxConvLocation : "YOU MUST SET THE FBX-CONV LOCATION");
                fbxConvLocationField.setEnabled(false);

                final JFileChooser fbxConvFileChooser = new JFileChooser();
                if(fbxConvLocation != null)
                        fbxConvFileChooser.setCurrentDirectory(new File(fbxConvLocation));
                fbxConvFileChooser.setDialogTitle("Find the file: fbx-conv-win32.exe");
                fbxConvFileChooser.setDragEnabled(false);
                fbxConvFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                fbxConvFileChooser.setAcceptAllFileFilterUsed(false);
                fbxConvFileChooser.setFileFilter(new FileFilter() {
                        @Override
                        public boolean accept(File f) {
                                return f.isDirectory() || isValidFbxConvFileLocation(f);
                        }

                        @Override
                        public String getDescription() {
                                return fbxConvExecName;
                        }
                });

                JButton browseFbxConvButton = new JButton("Browse...");
                browseFbxConvButton.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                                int returnVal = fbxConvFileChooser.showOpenDialog(desktopLauncher.frame);
                                if (returnVal == JFileChooser.APPROVE_OPTION) {
                                        File f = fbxConvFileChooser.getSelectedFile();
                                        String absPath = f.getAbsolutePath();
                                        fbxConvLocationField.setText(absPath);
                                        desktopLauncher.prefs.put(prefsKey, absPath);
                                        fbxConvLocation = absPath;
                                }
                        }
                });
                fbxConvToolBrowsePanel.add(browseFbxConvButton);


        }

        private boolean isValidFbxConvFileLocation(File f){
                String name = f.getName().toLowerCase();
                if (!(name.endsWith("fbx-conv-win32.exe") || name.endsWith("fbx-conv-lin64") || name.endsWith("fbx-conv-mac")))
                        return false;

                String path = f.getAbsolutePath();
                try {
                        Process proc = Runtime.getRuntime().exec(path,null,null);
                        String output = DesktopLauncher.processOutput(proc);
                        return output.contains("fbx-conv");
                } catch (IOException e) {
                        //e.printStackTrace();
                        return false;
                }
        }

        public boolean hasValidValue(){
                return fbxConvLocation != null;
        }

        public String getAbsolutePath(){
                return fbxConvLocation;
        }

        public String getName(){
                return fbxConvName;
        }

}

