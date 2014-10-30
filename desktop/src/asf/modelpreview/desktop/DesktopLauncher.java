package asf.modelpreview.desktop;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl.LwjglAWTCanvas;
import asf.modelpreview.ModelPreviewApp;
import com.badlogic.gdx.utils.GdxRuntimeException;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;
import javax.swing.plaf.basic.BasicFileChooserUI;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
                Runtime.getRuntime().addShutdownHook(new Thread(){
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

                frame. setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
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
                                centerTabbedPane.addTab("3D Preview",null,canvas.getCanvas(),"3D Preview");
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
                                        westLowerToolPane.addTab("Configuration",null,configPanel,"Configure fbx-conv");

                                        fbxConvLocationBox = new FileChooserFbxConv(this, S_fbxConvLocation, configPanel);

                                        flipTextureCoords = new BooleanConfigPanel(this,B_flipVTextureCoords,configPanel,
                                                "Flip V Texture Coordinates", false);
                                        packVertexColors = new BooleanConfigPanel(this,B_packVertexColorsToOneFloat,configPanel,
                                                "Pack vertex colors to one float", false);
                                        maxVertxPanel = new NumberConfigPanel(this, I_maxVertPerMesh,configPanel,
                                                "Max Verticies per mesh (k)",32,1,50,1);
                                        maxBonesPanel = new NumberConfigPanel(this,I_maxBonePerNodepart, configPanel,
                                                "Max Bones per nodepart",12,1,50,1);
                                        maxBonesWeightsPanel = new NumberConfigPanel(this,I_maxBoneWeightPerVertex, configPanel,
                                                "Max Bone Weights per vertex",4,1,50,1);
                                        outputFileTypeBox = new ComboStringConfigPanel(this,S_outputFileType,configPanel,
                                                "Output Format","G3DB",new String[]{"G3DB","G3DJ"});



                                }
                                // Viewport Settings
                                {

                                        JPanel viewportSettingsPanel = new JPanel();
                                        westLowerToolPane.addTab("Viewport Settings",null,viewportSettingsPanel,"Viewport Settings");

                                        environmentLightingBox = new BooleanConfigPanel(this, B_environmentLighting,viewportSettingsPanel,
                                                "Environment Lighting", true){
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
                frame.setSize(Math.round(width*.75f), Math.round(height*.75f));
                frame.setLocationRelativeTo(null);

                frame.setVisible(true);


        }


        private void logText(final String text){
                SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                                if(outputTextPane == null){
                                        System.out.println(text);
                                        return;
                                }
                                outputTextPane.setText(outputTextPane.getText()+"\n"+text);
                        }
                });
        }

        private void logTextError(Exception e){

                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                logTextError(sw.toString());
        }

        private void logTextError(final String text){
                SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                                if(outputTextPane == null){
                                        System.err.println(text);
                                        return;
                                }
                                outputTextPane.setText(outputTextPane.getText()+"\n"+text);
                                centerTabbedPane.setSelectedIndex(2);
                        }
                });
        }






        protected void previewFile(final File f){
                threadPool.submit(new Callable<Void>() {
                        @Override
                        public Void call() throws Exception {
                                // TODO: send something to the modelPreviewApp so itll show a loading message

                                final File newF= convertFile(f, true);
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

        private File convertFile(File f, boolean temp){
                String srcPath = f.getAbsolutePath();
                String srcLower = srcPath.toLowerCase();
                if(srcLower.endsWith(".g3db") || srcLower.endsWith(".g3dj")){
                        return f; // Already in desirable format, return the same file
                }

                if(!fbxConvLocationBox.hasValidValue()){
                        throw new IllegalStateException("fbx-conv is not yet configured.");
                }

                File targetDir = f.getParentFile();

                String dstPath =  targetDir+fbxConvLocationBox.dirSeperator+ (temp ?"libgdx-model-viewer.temp.g3dj": stripExtension(f.getName()));
                File convertedFile = new File(dstPath);

                try {
                        logText("-----------------------------------");
                        String[] s = new String[]{fbxConvLocationBox.getName(), "-v", f.getName(), convertedFile.getName()};
                        ProcessBuilder p =new ProcessBuilder(fbxConvLocationBox.getAbsolutePath(),"-v",srcPath,dstPath);

                        logText(stringArrayToString(s) + "\n");
                        String output = processOutput(p.start());
                        logText(output);
                } catch (IOException e) {
                        logTextError(e);
                        return null;
                }

                return convertedFile;
        }

        private static String stripExtension (String str) {
                if (str == null) return null;
                int pos = str.lastIndexOf(".");
                if (pos == -1) return str;
                return str.substring(0, pos);
        }

        private static String stringArrayToString(String[] stringArray){
                if(stringArray == null || stringArray.length==0)
                        return "";
                String output="";
                for (String s : stringArray) {
                        output+=s+" ";
                }
                return output.substring(0, output.length()-1);
        }

        protected static String processOutput(Process proc) throws java.io.IOException {
                java.io.InputStream is = proc.getInputStream();
                java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
                String val = "";
                if (s.hasNext()) {
                        val = s.next();
                }
                else {
                        val = "";
                }
                return val;
        }
}
