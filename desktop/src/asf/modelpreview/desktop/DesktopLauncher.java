package asf.modelpreview.desktop;

import asf.modelpreview.ModelPreviewApp;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl.LwjglAWTCanvas;
import com.badlogic.gdx.graphics.g3d.model.Animation;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.prefs.Preferences;

public class DesktopLauncher implements ModelPreviewApp.DesktopAppResolver {

        public static void main(String[] arg) {

                SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                                new DesktopLauncher();
                        }
                });
        }

        public final Preferences prefs;
        private final ExecutorService threadPool;
        protected final JFrame frame;

        private FileChooserSideBar fileChooser;
        private JTabbedPane westLowerToolPane;
        private FileChooserFbxConv fbxConvLocationBox;
        private BooleanConfigPanel flipTextureCoords, packVertexColors;
        private NumberConfigPanel maxVertxPanel, maxBonesPanel, maxBonesWeightsPanel;
        protected ComboStringConfigPanel outputFileTypeBox;
        protected JButton convertButton;
        private BooleanConfigPanel environmentLightingBox, backFaceCullingBox;
        private JComboBox animComboBox;
        private JScrollPane outputTextScrollPane;
        private JTextPane outputTextPane;
        private ModelPreviewApp modelPreviewApp;


        protected static final String S_folderLocation = "S_folderLocation";
        protected static final String I_fileFilter = "I_fileFilter";
        protected static final String B_alwaysConvert = "B_alwaysConvert";
        protected static final String B_automaticPreview = "B_automaticPreview";
        private static final String S_fbxConvLocation = "S_fbxConvLocation";
        private static final String B_flipVTextureCoords = "B_flipVTextureCoords";
        private static final String B_packVertexColorsToOneFloat = "B_packVertexColorsToOneFloat";
        private static final String I_maxVertPerMesh = "I_maxVertPerMesh";
        private static final String I_maxBonePerNodepart = "I_maxBonePerNodepart";
        private static final String I_maxBoneWeightPerVertex = "I_maxBoneWeightPerVertex";
        private static final String S_outputFileType = "S_outputFileType";
        private static final String S_batchConvertFileType = "S_batchConvertFileType";
        private static final String B_environmentLighting = "B_environmentLighting";
        private static final String B_backFaceCulling = "B_backFaceCulling";

        public DesktopLauncher() {
                prefs = Preferences.userRoot().node("LibGDXModelPreviewUtility");


                threadPool = Executors.newCachedThreadPool();
                Runtime.getRuntime().addShutdownHook(new Thread() {
                        @Override
                        public void run() {
                                threadPool.shutdownNow();
                        }
                });

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



                ///JPopupMenu.setDefaultLightWeightPopupEnabled(true);
                UIManager.put("FileChooser.readOnly", Boolean.TRUE);


                frame = new JFrame("LibGDX Model Preview Utility");
                TransferHandler handler = new TransferHandler() {
                        @Override
                        public boolean canImport(TransferSupport support) {
                                if (!support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                                        return false;
                                }
                                return true;
                        }

                        @Override
                        public boolean importData(TransferSupport support) {
                                Transferable t = support.getTransferable();
                                List<File> data;
                                try {
                                        data = (List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);
                                } catch (Exception e) {
                                        return false;
                                }
                                if(data.isEmpty()){
                                        return false;
                                }
                                convertFilesAsBatch(data);
                                return true;
                        }

                };
                frame.setTransferHandler(handler);
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                final Container container = frame.getContentPane();
                container.setLayout(new BorderLayout());

                // center tabbed pane
                {
                        modelPreviewApp = new ModelPreviewApp(this);
                        modelPreviewApp.backgroundColor.set(100 / 255f, 149 / 255f, 237 / 255f, 1f);
                        LwjglAWTCanvas canvas = new LwjglAWTCanvas(modelPreviewApp);
                        container.add(canvas.getCanvas(), BorderLayout.CENTER);
                }

                // Left Side Tool Bar
                {
                        JPanel westBasePanel = new JPanel(new BorderLayout());
                        container.add(westBasePanel, BorderLayout.WEST);

                        JPanel westUpperFileBrowsingPanel = new JPanel(new BorderLayout());
                        westBasePanel.add(westUpperFileBrowsingPanel, BorderLayout.NORTH);


                        // Source File File Chooser
                        {
                                fileChooser = new FileChooserSideBar(this, westUpperFileBrowsingPanel);
                        }


                        // File Conversion and File Preview Settings
                        {
                                westLowerToolPane = new JTabbedPane();
                                westBasePanel.add(westLowerToolPane, BorderLayout.CENTER);
                                westLowerToolPane.setTabPlacement(JTabbedPane.TOP);
                                // fbx-conv Configuration
                                {
                                        JPanel configPanel = new JPanel();
                                        BoxLayout bl = new BoxLayout(configPanel, BoxLayout.PAGE_AXIS);
                                        configPanel.setLayout(bl);
                                        JScrollPane configPanelScrollPane = new JScrollPane(configPanel);
                                        westLowerToolPane.addTab("Configuration", null, configPanelScrollPane, "Configure fbx-conv");

                                        fbxConvLocationBox = new FileChooserFbxConv(this, S_fbxConvLocation, configPanel);

                                        JPanel flipBase = new JPanel();
                                        configPanel.add(flipBase);
                                        flipTextureCoords = new BooleanConfigPanel(this, B_flipVTextureCoords, flipBase,
                                                "Flip V Texture Coordinates", false);

                                        maxVertxPanel = new NumberConfigPanel(this, I_maxVertPerMesh, configPanel,
                                                "Max Verticies per mesh (k)", 32, 1, 50, 1);
                                        maxBonesPanel = new NumberConfigPanel(this, I_maxBonePerNodepart, configPanel,
                                                "Max Bones per nodepart", 12, 1, 50, 1);
                                        maxBonesWeightsPanel = new NumberConfigPanel(this, I_maxBoneWeightPerVertex, configPanel,
                                                "Max Bone Weights per vertex", 4, 1, 50, 1);
                                        JPanel packBase = new JPanel();
                                        configPanel.add(packBase);
                                        packVertexColors = new BooleanConfigPanel(this, B_packVertexColorsToOneFloat, packBase,
                                                "Pack vertex colors to one float", false);
                                        outputFileTypeBox = new ComboStringConfigPanel(this, S_outputFileType, configPanel,
                                                "Output Format", "G3DB", new String[]{"G3DB", "G3DJ"}) {
                                                @Override
                                                protected void onChange() {
                                                        refreshConvertButtonText();
                                                }
                                        };

                                        JPanel buttonBase = new JPanel();
                                        configPanel.add(buttonBase);
                                        convertButton = new JButton("Choose a file to convert");
                                        buttonBase.add(convertButton);
                                        convertButton.setPreferredSize(new Dimension(400, 50));
                                        refreshConvertButtonText();
                                        convertButton.addActionListener(new ActionListener() {
                                                @Override
                                                public void actionPerformed(ActionEvent e) {
                                                        previewFile(fileChooser.getSelectedFile(), false);
                                                }
                                        });


                                }
                                // Viewport Settings
                                {

                                        JPanel viewportSettingsPanel = new JPanel();
                                        JScrollPane viewportSettingsPanelScrollPane = new JScrollPane(viewportSettingsPanel);
                                        westLowerToolPane.addTab("Viewport Settings", null, viewportSettingsPanelScrollPane, "Viewport Settings");
                                        BoxLayout bl = new BoxLayout(viewportSettingsPanel, BoxLayout.PAGE_AXIS);
                                        viewportSettingsPanel.setLayout(bl);

                                        JPanel baseEnvPanel = new JPanel();
                                        viewportSettingsPanel.add(baseEnvPanel);
                                        environmentLightingBox = new BooleanConfigPanel(this, B_environmentLighting, baseEnvPanel,
                                                "Environment Lighting", true) {
                                                @Override
                                                protected void onChange() {
                                                        modelPreviewApp.environmentLightingEnabled = isSelected();
                                                }
                                        };

                                        JPanel baseBackFacePanel = new JPanel();
                                        viewportSettingsPanel.add(baseBackFacePanel);
                                        backFaceCullingBox = new BooleanConfigPanel(this, B_backFaceCulling, baseBackFacePanel,
                                                "Back Face Culling", true) {
                                                @Override
                                                protected void onChange() {
                                                        Gdx.app.postRunnable(new Runnable() {
                                                                @Override
                                                                public void run() {
                                                                        modelPreviewApp.setBackFaceCulling(isSelected());
                                                                }
                                                        });
                                                }
                                        };

                                        JPanel baseAnimPanel = new JPanel();
                                        viewportSettingsPanel.add(baseAnimPanel);
                                        baseAnimPanel.add(new JLabel("Animation: "));
                                        animComboBox = new JComboBox();
                                        baseAnimPanel.add(animComboBox);

                                        animComboBox.setRenderer(new BasicComboBoxRenderer(){
                                                @Override
                                                public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                                                        if(value == null){
                                                                setText("<No Animation>");
                                                        }else{
                                                                Animation anim = (Animation) value;
                                                                setText(anim.id+"  -  "+anim.duration);
                                                        }
                                                        return this;
                                                }
                                        });

                                        animComboBox.addItem(null);


                                        animComboBox.addActionListener(new ActionListener() {
                                                @Override
                                                public void actionPerformed(ActionEvent e) {

                                                        modelPreviewApp.setAnimation((Animation)animComboBox.getSelectedItem());
                                                }
                                        });


                                        JPanel baseCamPanel = new JPanel();
                                        viewportSettingsPanel.add(baseCamPanel);
                                        JButton resetCamButton = new JButton("Reset Camera");
                                        baseCamPanel.add(resetCamButton);
                                        resetCamButton.addActionListener(new ActionListener() {
                                                @Override
                                                public void actionPerformed(ActionEvent e) {
                                                        Gdx.app.postRunnable(new Runnable() {
                                                                @Override
                                                                public void run() {
                                                                        modelPreviewApp.resetCam();
                                                                }
                                                        });
                                                }
                                        });

                                }

                                // Output Console
                                {
                                        outputTextPane = new JTextPane();
                                        outputTextScrollPane = new JScrollPane(outputTextPane);
                                        westLowerToolPane.addTab("Output Console", null, outputTextScrollPane, "Output");
                                }

                        }
                }


                frame.pack();

                Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                int height = screenSize.height;
                int width = screenSize.width;
                frame.setSize(Math.round(width * .75f), Math.round(height * .75f));
                frame.setLocationRelativeTo(null);

                frame.setVisible(true);


        }

        public void setAnimList(Array<Animation> animations){
                animComboBox.removeAllItems();
                animComboBox.addItem(null);
                if(animations==null)
                        return;

                for(Animation animName : animations){
                        animComboBox.addItem(animName);
                }
        }

        protected void refreshConvertButtonText() {
                if (convertButton == null) {
                        return;
                }
                if (fileChooser.isFileCanBeConverted(fileChooser.getSelectedFile())) {
                        String ext = outputFileTypeBox.getValue().toLowerCase();
                        String val = DesktopLauncher.stripExtension(fileChooser.getSelectedFile().getName()) + "." + ext;
                        convertButton.setText("Convert to: " + val);
                        convertButton.setEnabled(true);
                } else {
                        convertButton.setText("Choose a file to convert");
                        convertButton.setEnabled(false);
                }
        }

        private void logText(final String text) {
                SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                                if (outputTextPane == null) {
                                        System.out.println(text);
                                        return;
                                }
                                outputTextPane.setText(outputTextPane.getText() + "\n" + text);
                        }
                });
        }

        private void logTextError(Exception e) {

                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                logTextError(sw.toString());
        }

        private void logTextError(final String text) {
                SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                                if (outputTextPane == null) {
                                        System.err.println(text);
                                        return;
                                }
                                outputTextPane.setText(outputTextPane.getText() + "\n" + text);
                                westLowerToolPane.setSelectedComponent(outputTextScrollPane);
                        }
                });
        }

        private void convertFilesAsBatch(final List<File> files) {
                if(files.size() == 1 && !files.get(0).isDirectory()){
                        // a single non directory file was chosen, lets just select it
                        fileChooser.setSelectedFile(files.get(0), true);
                        return;
                }

                String[] options = new String[]{".fbx",".obj",".dae"};
                String dstExtension = outputFileTypeBox.getValue().equals("G3DJ") ? ".g3dj" : ".g3db";
                String msgAddition = files.size()==1 && files.get(0).isDirectory() ? " in "+files.get(0).getName() : "";
                final String srcExtension = (String) JOptionPane.showInputDialog(
                        frame,
                        "Convert all files"+msgAddition+" to "+dstExtension+" that have the following extension:\n\n WARNING: this will start converting your files and cannot be undone!",
                        "Batch model conversion",
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        options,
                        prefs.get(S_batchConvertFileType,".fbx"));



                if(srcExtension == null){
                        return;
                }

                prefs.put(S_batchConvertFileType,srcExtension);

                westLowerToolPane.setSelectedComponent(outputTextScrollPane);
                logText("---------Begin Batch File Conversion");

                threadPool.submit(new Callable<Void>() {
                        @Override
                        public Void call() throws Exception {
                                for(File file : files){
                                        convertFileRecursive(file, srcExtension);
                                }
                                return null;
                        }
                });


        }

        protected void previewFile(final File f, final boolean temp) {
                threadPool.submit(new Callable<Void>() {
                        @Override
                        public Void call() throws Exception {
                                Gdx.app.postRunnable(new Runnable() {
                                        @Override
                                        public void run() {
                                                if (temp)
                                                        modelPreviewApp.showLoadingText("Loading...");
                                                else
                                                        modelPreviewApp.showLoadingText("Converting\n" + f.getName());
                                        }
                                });


                                final File newF = convertFile(f, temp, true);

                                Gdx.app.postRunnable(new Runnable() {
                                        @Override
                                        public void run() {
                                                if (newF == null || newF.isDirectory()) {
                                                        modelPreviewApp.previewFile(null);
                                                } else {
                                                        try {
                                                                modelPreviewApp.previewFile(newF);
                                                        } catch (GdxRuntimeException ex) {
                                                                logTextError("Error while previewing file: " + f.getName());
                                                                logTextError(ex);
                                                                modelPreviewApp.previewFile(null);
                                                        }

                                                }

                                                if (temp && newF != f && newF != null && !newF.isDirectory()) { // only delete newF if it is a temp file that was made in convertFile
                                                        newF.delete();
                                                }

                                                SwingUtilities.invokeLater(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                                fileChooser.refreshFileChooserList();
                                                        }
                                                });
                                        }
                                });

                                return null;
                        }
                });
        }

        private void convertFileRecursive(File f, String srcExtension){

                if(f.isDirectory()){
                        File[] files = f.listFiles();
                        for (File file : files) {
                                convertFileRecursive(file, srcExtension);
                        }
                }else{
                        if(f.getName().toLowerCase().endsWith(srcExtension)){
                                File outputFile = convertFile(f, false, false);
                                if(outputFile != null && outputFile != f){
                                        logText(f.getAbsolutePath()+ "--> "+outputFile.getName());
                                }else{
                                        logTextError(f.getAbsolutePath() + "--> Error, could not convert!");
                                }
                        }
                }
        }

        private File convertFile(File f, boolean temp, boolean logDetailedOutput) {
                if (f == null || f.isDirectory()) {
                        return null; // not a model file
                }
                String srcPath = f.getAbsolutePath();
                String srcLower = srcPath.toLowerCase();
                if (srcLower.endsWith(".g3db") || srcLower.endsWith(".g3dj")) {
                        return f; // Already in desirable format, return the same file
                }

                if (!fbxConvLocationBox.hasValidValue()) {
                        if(logDetailedOutput)
                                logTextError("Can not convert file, fbx-conv location is not yet configured.");
                        return null;
                }

                File targetDir = f.getParentFile();
                String dstExtension = temp || outputFileTypeBox.getValue().equals("G3DJ") ? ".g3dj" : ".g3db";
                String dstPath = targetDir + fbxConvLocationBox.dirSeperator + (temp ? "libgdx-model-viewer.temp" : stripExtension(f.getName())) + dstExtension;
                File convertedFile = new File(dstPath);
                try {

                        if(logDetailedOutput)
                                logText("-----------------------------------");
                        ProcessBuilder p = new ProcessBuilder(fbxConvLocationBox.getAbsolutePath(), "-v");
                        if (flipTextureCoords.isSelected())
                                p.command().add("-f");
                        if (packVertexColors.isSelected())
                                p.command().add("-p");
                        p.command().add("-m");
                        p.command().add(maxVertxPanel.getString());
                        p.command().add("-b");
                        p.command().add(maxBonesPanel.getString());
                        p.command().add("-w");
                        p.command().add(maxBonesWeightsPanel.getString());
                        p.command().add(srcPath);
                        p.command().add(dstPath);
                        if(logDetailedOutput)
                                logText(shortenCommand(p.command(), fbxConvLocationBox.getName(), f.getName(), convertedFile.getName()) + "\n");
                        String output = processOutput(p.start());
                        if(logDetailedOutput)
                                logText(output);
                } catch (IOException e) {
                        logTextError(e);
                        return null;
                }

                return convertedFile;
        }

        protected static String stripExtension(String str) {
                if (str == null) return null;
                int pos = str.lastIndexOf(".");
                if (pos == -1) return str;
                return str.substring(0, pos);
        }

        private static String shortenCommand(List<String> command, String shortExecName, String shortSrcName, String shortDstName) {
                String output = shortExecName + " ";
                for (int i = 1; i < command.size() - 2; i++) {
                        output += command.get(i) + " ";
                }
                return output + " " + shortSrcName + " " + shortDstName;
        }

        private static String stringArrayToString(String[] stringArray) {
                if (stringArray == null || stringArray.length == 0)
                        return "";
                String output = "";
                for (String s : stringArray) {
                        if (s == null || s.isEmpty()) {
                                continue;
                        }
                        output += s + " ";
                }
                return output.substring(0, output.length() - 1);
        }

        protected static String processOutput(Process proc) throws java.io.IOException {
                java.io.InputStream is = proc.getInputStream();
                java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
                String val = "";
                if (s.hasNext()) {
                        val = s.next();
                } else {
                        val = "";
                }
                return val;
        }
}
