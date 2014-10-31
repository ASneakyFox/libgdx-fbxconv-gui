package asf.modelpreview.desktop;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.filechooser.FileFilter;
import javax.swing.plaf.basic.BasicFileChooserUI;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;

/**
 * Created by Danny on 10/30/2014.
 */
public class FileChooserSideBar {

        JFileChooser fileChooser;
        BasicFileFilter[] fileFilters;
        private BooleanConfigPanel alwaysConvert, automaticPreviewBox;
        private JButton previewFileButton;

        public FileChooserSideBar(final DesktopLauncher desktopLauncher,JPanel parentPanel) {

                fileChooser = new JFileChooser();
                parentPanel.add(fileChooser, BorderLayout.CENTER);
                fileChooser.setDialogTitle("Choose Assets Directory");
                fileChooser.setDialogType(JFileChooser.CUSTOM_DIALOG);
                fileChooser.setControlButtonsAreShown(false);
                fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
                fileChooser.setAcceptAllFileFilterUsed(true);
                fileChooser.setFileHidingEnabled(true);
                fileChooser.setAcceptAllFileFilterUsed(false);
                BasicFileChooserUI ui = (BasicFileChooserUI) fileChooser.getUI();
                ui.getNewFolderAction().setEnabled(false);
                fileFilters = new BasicFileFilter[]{
                        new BasicFileFilter("Autodesk *.fbx",".fbx"),
                        new BasicFileFilter("Wavefront *.obj", ".obj"),
                        new BasicFileFilter("Collada *.dae",".dae"),
                        new BasicFileFilter("LibGDX 3D Binary *.g3db",".g3db"),
                        new BasicFileFilter("LibGDX 3D Json *.g3dj",".g3dj"),
                        new BasicFileFilter("All Compatible LibGDX Files",".obj",".fbx",".dae",".g3db",".g3dj")
                };
                for (BasicFileFilter fileFilter : fileFilters) {
                        fileChooser.addChoosableFileFilter(fileFilter);
                }
                int prefFileFilter = desktopLauncher.prefs.getInt(DesktopLauncher.I_fileFilter,fileFilters.length-1);
                if(prefFileFilter <0 || prefFileFilter >= fileFilters.length){
                        prefFileFilter = fileFilters.length-1;
                }
                fileChooser.setFileFilter(fileFilters[prefFileFilter]);
                fileChooser.addPropertyChangeListener("fileFilterChanged",new PropertyChangeListener() {
                        @Override
                        public void propertyChange(PropertyChangeEvent evt) {

                                for (int i = 0; i < fileFilters.length; i++) {
                                        if(fileFilters[i] == evt.getNewValue()){
                                                desktopLauncher.prefs.putInt(DesktopLauncher.I_fileFilter,i);
                                                return;
                                        }
                                }
                        }
                });



                String loc = desktopLauncher.prefs.get(DesktopLauncher.S_folderLocation,null);
                if(loc != null)
                        fileChooser.setCurrentDirectory(new File(loc));

                fileChooser.addPropertyChangeListener("directoryChanged",new PropertyChangeListener() {
                        @Override
                        public void propertyChange(PropertyChangeEvent evt) {
                                File newVal = (File)evt.getNewValue();
                                desktopLauncher.prefs.put(DesktopLauncher.S_folderLocation,newVal.getAbsolutePath());
                        }
                });

                fileChooser.addPropertyChangeListener("SelectedFileChangedProperty", new PropertyChangeListener() {
                        @Override
                        public void propertyChange(PropertyChangeEvent evt) {
                                File fNew = (File)evt.getNewValue();

                                desktopLauncher.refreshConvertButtonText();

                                if(automaticPreviewBox.isSelected())
                                        desktopLauncher.previewFile(fNew, true);


                        }
                });

                //fileChooser.setAccessory(new JButton("convert"));

                // File preview options
                {
                        JPanel westSouthPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
                        parentPanel.add(westSouthPanel, BorderLayout.SOUTH);

                        alwaysConvert= new BooleanConfigPanel(desktopLauncher,DesktopLauncher.B_alwaysConvert,westSouthPanel, "Always Convert on Drag n' Drop", true){
                                @Override
                                protected void onChange() {

                                }
                        };
                        alwaysConvert.checkBox.setToolTipText("<html>Check this to convert the source file instead of previewing it first when dragging<br>(Preview is still shown after converting)</html>");


                        automaticPreviewBox = new BooleanConfigPanel(desktopLauncher,DesktopLauncher.B_automaticPreview,westSouthPanel, "Automatic Preview", true){
                                @Override
                                protected void onChange() {
                                        if(isSelected())
                                                desktopLauncher.previewFile(fileChooser.getSelectedFile(), true);
                                }
                        };



                        previewFileButton = new JButton("Preview File");
                        westSouthPanel.add(previewFileButton);
                        previewFileButton.addActionListener(new ActionListener() {
                                @Override
                                public void actionPerformed(final ActionEvent e) {
                                        desktopLauncher.previewFile(fileChooser.getSelectedFile(), true);
                                }
                        });

                }



        }

        public boolean isAlwaysConvert(){
                return alwaysConvert.isSelected();
        }


        public File getSelectedFile(){
                return fileChooser.getSelectedFile();
        }

        /**
         * returns true if this is an obj, fbx, or dae file
         * @param file
         * @return
         */
        public boolean isFileCanBeConverted(File file){
                if(file == null || file.isDirectory())
                        return false;

                String name = file.getName().toLowerCase();

                return name.endsWith(".obj") || name.endsWith(".fbx") || name.endsWith(".dae");
        }

        public void refreshFileChooserList(){
                fileChooser.rescanCurrentDirectory();
                //((javax.swing.plaf.basic.BasicDirectoryModel)list.getModel()).fireContentsChanged();
        }


}

