package asf.modelpreview.desktop;

import asf.modelpreview.ModelPreviewApp;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl.LwjglAWTCanvas;
import com.badlogic.gdx.utils.GdxRuntimeException;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.prefs.Preferences;

public class DesktopLauncher {

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

        private FileChooserFbxConv fbxConvLocationBox;
        private BooleanConfigPanel flipTextureCoords, packVertexColors;
        private NumberConfigPanel maxVertxPanel, maxBonesPanel, maxBonesWeightsPanel;
        private ComboStringConfigPanel outputFileTypeBox;
        private BooleanConfigPanel environmentLightingBox;
        private JTabbedPane centerTabbedPane, westLowerToolPane;
        private JTextPane outputTextPane;
        private BasicFileFilter[] fileFilters;
        private ModelPreviewApp modelPreviewApp;

        protected static final String S_folderLocation = "S_folderLocation";
        protected static final String I_fileFilter = "I_fileFilter";
        protected static final String B_automaticPreview = "B_automaticPreview";
        private static final String S_fbxConvLocation = "S_fbxConvLocation";
        private static final String B_flipVTextureCoords = "B_flipVTextureCoords";
        private static final String B_packVertexColorsToOneFloat = "B_packVertexColorsToOneFloat";
        private static final String I_maxVertPerMesh = "I_maxVertPerMesh";
        private static final String I_maxBonePerNodepart = "I_maxBonePerNodepart";
        private static final String I_maxBoneWeightPerVertex = "I_maxBoneWeightPerVertex";
        private static final String S_outputFileType = "S_outputFileType";
        private static final String B_environmentLighting = "B_environmentLighting";


        public DesktopLauncher() {
                prefs = Preferences.userRoot().node("LibGDXModelPreviewUtility");


                threadPool = Executors.newCachedThreadPool();
                Runtime.getRuntime().addShutdownHook(new Thread() {
                        @Override
                        public void run() {
                                System.out.println("Shutdown Now");
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
                UIManager.put("FileChooser.readOnly", Boolean.TRUE);


                frame = new JFrame("LibGDX Model Preview Utility");

                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                final Container container = frame.getContentPane();
                container.setLayout(new BorderLayout());

                // center tabbed pane
                {
                        centerTabbedPane = new JTabbedPane();
                        container.add(centerTabbedPane, BorderLayout.CENTER);

                        // 3D Preview
                        {
                                modelPreviewApp = new ModelPreviewApp();
                                modelPreviewApp.backgroundColor.set(100 / 255f, 149 / 255f, 237 / 255f, 1f);
                                LwjglAWTCanvas canvas = new LwjglAWTCanvas(modelPreviewApp);
                                centerTabbedPane.addTab("3D Preview", null, canvas.getCanvas(), "3D Preview");
                        }


                        // G3DJ viewer
                        {
                                JTextPane g3djTextView = new JTextPane();
                                JScrollPane scrollPane = new JScrollPane(g3djTextView);
                                centerTabbedPane.addTab("G3DJ", null, scrollPane, "View the G3DJ file");
                        }


                        // Output Console
                        {
                                outputTextPane = new JTextPane();
                                JScrollPane scrollPane = new JScrollPane(outputTextPane);
                                centerTabbedPane.addTab("Output Console", null, scrollPane, "Output");
                        }


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
                                        configPanel.setLayout(new BoxLayout(configPanel, BoxLayout.PAGE_AXIS));
                                        westLowerToolPane.addTab("Configuration", null, configPanel, "Configure fbx-conv");

                                        fbxConvLocationBox = new FileChooserFbxConv(this, S_fbxConvLocation, configPanel);

                                        flipTextureCoords = new BooleanConfigPanel(this, B_flipVTextureCoords, configPanel,
                                                "Flip V Texture Coordinates", false);
                                        packVertexColors = new BooleanConfigPanel(this, B_packVertexColorsToOneFloat, configPanel,
                                                "Pack vertex colors to one float", false);
                                        maxVertxPanel = new NumberConfigPanel(this, I_maxVertPerMesh, configPanel,
                                                "Max Verticies per mesh (k)", 32, 1, 50, 1);
                                        maxBonesPanel = new NumberConfigPanel(this, I_maxBonePerNodepart, configPanel,
                                                "Max Bones per nodepart", 12, 1, 50, 1);
                                        maxBonesWeightsPanel = new NumberConfigPanel(this, I_maxBoneWeightPerVertex, configPanel,
                                                "Max Bone Weights per vertex", 4, 1, 50, 1);
                                        outputFileTypeBox = new ComboStringConfigPanel(this, S_outputFileType, configPanel,
                                                "Output Format", "G3DB", new String[]{"G3DB", "G3DJ"});


                                }
                                // Viewport Settings
                                {

                                        JPanel viewportSettingsPanel = new JPanel();
                                        westLowerToolPane.addTab("Viewport Settings", null, viewportSettingsPanel, "Viewport Settings");

                                        environmentLightingBox = new BooleanConfigPanel(this, B_environmentLighting, viewportSettingsPanel,
                                                "Environment Lighting", true) {
                                                @Override
                                                protected void onChange() {
                                                        modelPreviewApp.environmentLightingEnabled = isSelected();
                                                }
                                        };

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
                                centerTabbedPane.setSelectedIndex(2);
                        }
                });
        }


        protected void previewFile(final File f) {


                threadPool.submit(new Callable<Void>() {
                        @Override
                        public Void call() throws Exception {
                                Gdx.app.postRunnable(new Runnable() {
                                        @Override
                                        public void run() {
                                                modelPreviewApp.showLoadingText();
                                        }
                                });


                                final File newF = convertFile(f, true);
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
                                                        if (newF != f)
                                                                newF.delete(); // only delete newF if it is a temp file that was made in convertFile
                                                }

                                        }
                                });
                                return null;
                        }
                });
        }

        private File convertFile(File f, boolean temp) {
                if(f == null || f.isDirectory()){
                        return null; // not a model file
                }
                String srcPath = f.getAbsolutePath();
                String srcLower = srcPath.toLowerCase();
                if (srcLower.endsWith(".g3db") || srcLower.endsWith(".g3dj")) {
                        return f; // Already in desirable format, return the same file
                }

                if (!fbxConvLocationBox.hasValidValue()) {
                        logTextError("Can not convert file, fbx-conv location is not yet configured.");
                        return null;
                }

                File targetDir = f.getParentFile();
                String dstExtension = temp || outputFileTypeBox.getValue().equals("G3DJ") ? ".g3dj" : ".g3db";
                String dstPath = targetDir + fbxConvLocationBox.dirSeperator + (temp ? "libgdx-model-viewer.temp" : stripExtension(f.getName())) + dstExtension;
                File convertedFile = new File(dstPath);
                try {
                        logText("-----------------------------------");
                        ProcessBuilder p = new ProcessBuilder(fbxConvLocationBox.getAbsolutePath(),"-v");
                        if(flipTextureCoords.isSelected())
                                p.command().add("-f");
                        if(packVertexColors.isSelected())
                                p.command().add("-p");
                        p.command().add("-m");
                        p.command().add(maxVertxPanel.getString());
                        p.command().add("-b");
                        p.command().add(maxBonesPanel.getString());
                        p.command().add("-w");
                        p.command().add(maxBonesWeightsPanel.getString());
                        p.command().add(srcPath);
                        p.command().add(dstPath);

                        logText(shortenCommand(p.command(), fbxConvLocationBox.getName(), f.getName(), convertedFile.getName()) + "\n");
                        String output = processOutput(p.start());
                        logText(output);
                } catch (IOException e) {
                        logTextError(e);
                        return null;
                }

                return convertedFile;
        }

        private static String stripExtension(String str) {
                if (str == null) return null;
                int pos = str.lastIndexOf(".");
                if (pos == -1) return str;
                return str.substring(0, pos);
        }

        private static String shortenCommand(List<String> command, String shortExecName, String shortSrcName, String shortDstName){
                String output = shortExecName + " ";
                for (int i = 1; i < command.size()-2; i++) {
                        output += command.get(i) + " ";
                }
                return output+" "+shortSrcName+" "+shortDstName;
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
