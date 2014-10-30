package asf.modelpreview.desktop;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl.LwjglAWTCanvas;
import asf.modelpreview.ModelPreviewApp;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.GdxRuntimeException;
import javafx.stage.FileChooser;


import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileView;
import javax.swing.plaf.basic.BasicFileChooserUI;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.prefs.Preferences;

public class DesktopLauncher {

	public static void main (String[] arg) {

                SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                                new DesktopLauncher();
                        }
                });
	}

        private Preferences prefs;
        private JFrame frame;
        private JFileChooser fileChooser;
        private JCheckBox automaticPreviewBox;
        private JTextPane outputTextPane;
        private BasicFileFilter[] fileFilters;
        private ModelPreviewApp modelPreviewApp;

        private static final String S_folderLocation = "S_folderLocation";
        private static final String S_fbxConvLocation = "S_fbxConvLocation";
        private static final String B_environmentLighting = "B_environmentLighting";
        private static final String B_automaticPreview = "B_automaticPreview";
        private static final String I_fileFilter = "I_fileFilter";

        public DesktopLauncher() {
                try {
                        for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                                if ("Nimbus".equals(info.getName())) {
                                        UIManager.setLookAndFeel(info.getClassName());
                                        break;
                                }
                        }
                } catch (Exception e) {
                        // If Nimbus is not available, you can set the GUI to another look and feel.
                }
                UIManager.put("FileChooser.readOnly", Boolean.TRUE);

                prefs = Preferences.userRoot().node("LibGDXModelPreviewUtility");
                frame = new JFrame("LibGDX Model Preview Utility");

                frame. setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                final Container container = frame.getContentPane();
                container.setLayout(new BorderLayout());

                modelPreviewApp = new ModelPreviewApp();
                modelPreviewApp.backgroundColor.set(100 / 255f, 149 / 255f, 237 / 255f, 1f);

                LwjglAWTCanvas canvas = new LwjglAWTCanvas(modelPreviewApp);
                container.add(canvas.getCanvas(), BorderLayout.CENTER);

                JPanel westBasePanel = new JPanel(new BorderLayout());
                container.add(westBasePanel, BorderLayout.WEST);

                JPanel westUpperFileBrowsingPanel = new JPanel(new BorderLayout());
                westBasePanel.add(westUpperFileBrowsingPanel, BorderLayout.NORTH);
                {


                        fileChooser = new JFileChooser();
                        westUpperFileBrowsingPanel.add(fileChooser, BorderLayout.CENTER);
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
                        int prefFileFilter = prefs.getInt(I_fileFilter,fileFilters.length-1);
                        if(prefFileFilter <0 || prefFileFilter >= fileFilters.length){
                                prefFileFilter = fileFilters.length-1;
                        }
                        fileChooser.setFileFilter(fileFilters[prefFileFilter]);
                        fileChooser.addPropertyChangeListener("fileFilterChanged",new PropertyChangeListener() {
                                @Override
                                public void propertyChange(PropertyChangeEvent evt) {

                                        for (int i = 0; i < fileFilters.length; i++) {
                                                if(fileFilters[i] == evt.getNewValue()){
                                                        prefs.putInt(I_fileFilter,i);
                                                        return;
                                                }
                                        }
                                }
                        });



                        String loc = prefs.get(S_folderLocation,null);
                        if(loc != null)
                                fileChooser.setCurrentDirectory(new File(loc));

                        fileChooser.addPropertyChangeListener("directoryChanged",new PropertyChangeListener() {
                                @Override
                                public void propertyChange(PropertyChangeEvent evt) {
                                        File newVal = (File)evt.getNewValue();
                                        prefs.put(S_folderLocation,newVal.getAbsolutePath());
                                }
                        });

                        fileChooser.addPropertyChangeListener("SelectedFileChangedProperty", new PropertyChangeListener() {
                                @Override
                                public void propertyChange(PropertyChangeEvent evt) {
                                        if(!automaticPreviewBox.isSelected())
                                            return;

                                        previewFile((File)evt.getNewValue());
                                }
                        });

                        //fileChooser.setAccessory(new JButton("convert"));

                        {
                                JPanel westSouthPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
                                westUpperFileBrowsingPanel.add(westSouthPanel, BorderLayout.SOUTH);

                                automaticPreviewBox = new JCheckBox("Automatic Preview");
                                westSouthPanel.add(automaticPreviewBox);
                                automaticPreviewBox.setSelected(prefs.getBoolean(B_automaticPreview,false));

                                automaticPreviewBox.addItemListener(new ItemListener() {
                                        @Override
                                        public void itemStateChanged(ItemEvent e) {
                                                prefs.putBoolean(B_automaticPreview, automaticPreviewBox.isSelected());
                                        }
                                });


                                JButton previewFileButton = new JButton("Preview File");
                                westSouthPanel.add(previewFileButton);
                                previewFileButton.addActionListener(new ActionListener() {
                                        @Override
                                        public void actionPerformed(final ActionEvent e) {
                                                previewFile(fileChooser.getSelectedFile());
                                        }
                                });

                        }

                }




                JTabbedPane westLowerToolPane = new JTabbedPane();
                westBasePanel.add(westLowerToolPane, BorderLayout.CENTER);

                {
                        westLowerToolPane.setTabPlacement(JTabbedPane.TOP);
                        // Configuration
                        {
                                JPanel configPanel = new JPanel();
                                configPanel.setLayout(new BoxLayout(configPanel, BoxLayout.PAGE_AXIS));
                                westLowerToolPane.addTab("Configuration",null,configPanel,"Configure fbx-conv");

                                {
                                        JPanel fbxConvToolBrowsePanel = new JPanel();
                                        configPanel.add(fbxConvToolBrowsePanel);

                                        final JTextField fbxConvLocationField = new JTextField();
                                        fbxConvToolBrowsePanel.add(fbxConvLocationField);
                                        String fbxConvLocation = prefs.get(S_fbxConvLocation, null);
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
                                                        if(f.isDirectory())
                                                                return true;
                                                        String name = f.getName().toLowerCase();
                                                        return (name.endsWith("fbx-conv-win32.exe") || name.endsWith("fbx-conv-lin64") || name.endsWith("fbx-conv-mac")) && isValidFbxConvFileLocation(f);
                                                }

                                                @Override
                                                public String getDescription() {
                                                        return "fbx-conv-win32.exe";
                                                }
                                        });

                                        JButton browseFbxConvButton = new JButton("Browse...");
                                        browseFbxConvButton.addActionListener(new ActionListener() {
                                                @Override
                                                public void actionPerformed(ActionEvent e) {
                                                        int returnVal = fbxConvFileChooser.showOpenDialog(frame);

                                                        if (returnVal == JFileChooser.APPROVE_OPTION) {
                                                                File f = fbxConvFileChooser.getSelectedFile();
                                                                fbxConvLocationField.setText(f.getAbsolutePath());
                                                                prefs.put(S_fbxConvLocation, f.getAbsolutePath());
                                                        } else {

                                                        }
                                                }
                                        });
                                        fbxConvToolBrowsePanel.add(browseFbxConvButton);
                                }


                                JCheckBox flipTextureCoords = new JCheckBox("Flip V Texture Coordinates");
                                configPanel.add(flipTextureCoords);

                                JCheckBox packVertexColors = new JCheckBox("Pack vertex colors to one float");
                                configPanel.add(packVertexColors);

                                {
                                        JPanel maxVertsPanel = new JPanel();
                                        configPanel.add(maxVertsPanel);

                                        JLabel maxVertsLabel = new JLabel("Max Verticies per mesh (k)");
                                        maxVertsPanel.add(maxVertsLabel);

                                        final JSpinner maxVertsSpinner = new JSpinner();
                                        maxVertsSpinner.setValue(32);
                                        maxVertsPanel.add(maxVertsSpinner);

                                        JButton resetToDefault = new JButton("Reset To Default");
                                        maxVertsPanel.add(resetToDefault);
                                }

                                {
                                        JPanel maxBonesPanel = new JPanel();
                                        configPanel.add(maxBonesPanel);

                                        JLabel maxBonesLabel = new JLabel("Max Bones per nodepart");
                                        maxBonesPanel.add(maxBonesLabel);

                                        final JSpinner maxBonesSpinner = new JSpinner();
                                        maxBonesSpinner.setValue(12);
                                        maxBonesPanel.add(maxBonesSpinner);

                                        JButton resetToDefault = new JButton("Reset To Default");
                                        maxBonesPanel.add(resetToDefault);
                                }

                                {
                                        JPanel maxBoneWeightsPanel = new JPanel();
                                        configPanel.add(maxBoneWeightsPanel);

                                        JLabel maxBonesLabel = new JLabel("Max Bone Weights per vertex");
                                        maxBoneWeightsPanel.add(maxBonesLabel);

                                        final JSpinner maxBoneWeights = new JSpinner();
                                        maxBoneWeights.setValue(4);
                                        maxBoneWeightsPanel.add(maxBoneWeights);

                                        JButton resetToDefault = new JButton("Reset To Default");
                                        maxBoneWeightsPanel.add(resetToDefault);
                                }

                                {
                                        JPanel outputFormatPanel = new JPanel();
                                        configPanel.add(outputFormatPanel);

                                        JLabel maxBonesLabel = new JLabel("Output Format");
                                        outputFormatPanel.add(maxBonesLabel);

                                        JComboBox outFileTypeBox = new JComboBox();
                                        outFileTypeBox.setEditable(false);
                                        outFileTypeBox.addItem("G3DB");
                                        outFileTypeBox.addItem("G3DJ");
                                        outputFormatPanel.add(outFileTypeBox);
                                }







                        }
                        // Viewport Settings
                        {
                                JPanel viewportSettingsPanel = new JPanel();
                                westLowerToolPane.addTab("Viewport Settings",null,viewportSettingsPanel,"Viewport Settings");
                                final JCheckBox environmentLightingBox = new JCheckBox("Environment Lighting");
                                viewportSettingsPanel.add(environmentLightingBox);
                                environmentLightingBox.setSelected(prefs.getBoolean(B_environmentLighting,false));
                                modelPreviewApp.environmentLightingEnabled = environmentLightingBox.isSelected();

                                environmentLightingBox.addItemListener(new ItemListener() {
                                        @Override
                                        public void itemStateChanged(ItemEvent e) {
                                                modelPreviewApp.environmentLightingEnabled = environmentLightingBox.isSelected();
                                                prefs.putBoolean(B_environmentLighting, environmentLightingBox.isSelected());
                                        }
                                });
                        }



                        JTextPane g3djTextView = new JTextPane();
                        westLowerToolPane.addTab("G3DJ", null, g3djTextView, "View the G3DJ file");

                        outputTextPane = new JTextPane();
                        westLowerToolPane.addTab("Output Console", null, outputTextPane, "Output");

                }





                frame.pack();

                Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                int height = screenSize.height;
                int width = screenSize.width;
                frame.setSize(Math.round(width*.75f), Math.round(height*.75f));
                frame.setLocationRelativeTo(null);

                frame.setVisible(true);


        }

        private void logText(String text){
                outputTextPane.setText(outputTextPane.getText()+"\n"+text);
        }

        private boolean isValidFbxConvFileLocation(File f){
                return true;
        }

        private void previewFile(final File f){
                Gdx.app.postRunnable(new Runnable() {
                        @Override
                        public void run() {
                                if(f == null || f.isDirectory()){
                                        modelPreviewApp.previewFile(null);
                                }else{
                                        try{
                                                modelPreviewApp.previewFile(f);
                                        }catch(GdxRuntimeException ex){
                                                StringWriter sw = new StringWriter();
                                                PrintWriter pw = new PrintWriter(sw);
                                                ex.printStackTrace(pw);
                                                sw.toString(); // stack trace as a string

                                                logText("Couldnt Load File: "+f.getName()+"\n"+sw.toString());

                                                modelPreviewApp.previewFile(null);

                                        }

                                }
                        }
                });
        }
}
