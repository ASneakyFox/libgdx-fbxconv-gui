package asf.modelpreview.desktop;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.plaf.basic.BasicFileChooserUI;
import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.net.URI;

/**
 * Created by Danny on 10/30/2014.
 */
public class FileChooserSideBar {

        final DesktopLauncher desktopLauncher;
        JFileChooser fileChooser;
        BasicFileFilter[] fileFilters;
        private BooleanConfigPanel alwaysConvert, automaticPreviewBox;
        private JButton previewFileButton;

        private PropertyChangeListener selectedFilePropertyChange;

        public FileChooserSideBar(final DesktopLauncher desktopLauncher,JPanel parentPanel) {
                this.desktopLauncher = desktopLauncher;
                fileChooser = new JFileChooser();
                parentPanel.add(fileChooser, BorderLayout.CENTER);
                fileChooser.setDialogTitle("Choose Assets Directory");
                fileChooser.setDialogType(JFileChooser.CUSTOM_DIALOG);
                fileChooser.setControlButtonsAreShown(false);
                fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
                fileChooser.setAcceptAllFileFilterUsed(true);
                fileChooser.setFileHidingEnabled(true);
                fileChooser.setAcceptAllFileFilterUsed(false);
                if(fileChooser.getUI() instanceof  BasicFileChooserUI){
                        BasicFileChooserUI ui = (BasicFileChooserUI) fileChooser.getUI();
                        ui.getNewFolderAction().setEnabled(false);
                }

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

                selectedFilePropertyChange = new PropertyChangeListener() {
                        @Override
                        public void propertyChange(PropertyChangeEvent evt) {
                                desktopLauncher.refreshConvertButtonText();
                                if(automaticPreviewBox.isSelected())
                                        desktopLauncher.previewFile(fileChooser.getSelectedFile(), true);

                        }
                };
                fileChooser.addPropertyChangeListener("SelectedFileChangedProperty",selectedFilePropertyChange );


                //fileChooser.setAccessory(new JButton("convert"));

                // File preview options
                {
                        JPanel westSouthPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
                        parentPanel.add(westSouthPanel, BorderLayout.SOUTH);

                        JButton aboutButton = new JButton("About");
                        westSouthPanel.add(aboutButton);

                        aboutButton.addActionListener(new ActionListener() {
                                @Override
                                public void actionPerformed(ActionEvent e) {
                                        Object[] options = {
                                                "View on Github",
                                                "Ok"};
                                        int n = JOptionPane.showOptionDialog(desktopLauncher.frame,
                                                "This is a simple GUI to help make it easier get your 3D models ready for your LibGDX game" +
                                                        "\n\nYou must have fbx-conv downloaded and it's libraries configured in order to use the file conversion function." +
                                                        "\n\nIf you need help or want more information about this software then visit its Github page.",
                                                "About LibGDX fbx-conv Gui",
                                                JOptionPane.YES_NO_OPTION,
                                                JOptionPane.QUESTION_MESSAGE,
                                                null,
                                                options,
                                                options[1]);

                                        try {
                                                Desktop.getDesktop().browse(new URI("https://github.com/ASneakyFox/libgdx-fbxconv-gui"));
                                        } catch (Throwable t) {
                                                JOptionPane.showMessageDialog(desktopLauncher.frame,"I couldnt open your browser, write this down:\n\nhttps://github.com/ASneakyFox/libgdx-fbxconv-gui");
                                        }
                                }
                        });

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


        public void setSelectedFile(File file){
                setSelectedFile(file, false);
        }
        public void setSelectedFile(File file, boolean setFromDragnDrop){

                // Remove and re-add the listener to avoid issues with redropping the same file
                fileChooser.removePropertyChangeListener("SelectedFileChangedProperty",selectedFilePropertyChange);

                boolean convertInsteadOfPreview = setFromDragnDrop && isAlwaysConvert();
                fileChooser.setSelectedFile(file);
                desktopLauncher.refreshConvertButtonText();
                if(automaticPreviewBox.isSelected())
                        desktopLauncher.previewFile(fileChooser.getSelectedFile(), !convertInsteadOfPreview);

                fileChooser.addPropertyChangeListener("SelectedFileChangedProperty",selectedFilePropertyChange );



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

