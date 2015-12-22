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
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.SpringLayout;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Desktop;
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
import java.net.URI;
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
        private JTabbedPane mainTabbedPane;
        private FileChooserFbxConv fbxConvLocationBox;
        private BooleanConfigPanel flipTextureCoords, packVertexColors;
        private NumberConfigPanel maxVertxPanel, maxBonesPanel, maxBonesWeightsPanel;
        protected ComboStringConfigPanel outputFileTypeBox;
        private BooleanConfigPanel environmentLightingBox, backFaceCullingBox, alphaBlendingBox;
        private BooleanIntegerConfigPanel alphaTestBox;
        private JComboBox<Animation> animComboBox;
        private JScrollPane outputTextScrollPane;
        private JTextPane outputTextPane;
        private ModelPreviewApp modelPreviewApp;


        protected static final String S_folderLocation = "S_folderLocation";
        protected static final String I_fileFilter = "I_fileFilter";
        //protected static final String B_alwaysConvert = "B_alwaysConvert";
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
        private static final String B_alphaBlending = "B_alphaBlending";
        private static final String B_alphaTest = "B_alphaTest";
        private static final String I_alphaTest = "I_alphaTest";

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
                frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
                final Container container = frame.getContentPane();
                ///container.setLayout(new BorderLayout());
		container.setLayout(new BoxLayout(container, BoxLayout.LINE_AXIS));

		mainTabbedPane = new JTabbedPane();
		mainTabbedPane.setTabPlacement(JTabbedPane.TOP);
		mainTabbedPane.setPreferredSize(new Dimension(525,1000));
		mainTabbedPane.setMaximumSize(new Dimension(550,2000));
		container.add(mainTabbedPane);

                // center tabbed pane
                {
                        modelPreviewApp = new ModelPreviewApp(this);
                        modelPreviewApp.backgroundColor.set(100 / 255f, 149 / 255f, 237 / 255f, 1f);
                        LwjglAWTCanvas canvas = new LwjglAWTCanvas(modelPreviewApp);
                        container.add(canvas.getCanvas());
                }

                // Left Side Tool Bar
                {

			// fbx-conv Configuration
			{
				JPanel configPanel = new JPanel();
				BoxLayout bl = new BoxLayout(configPanel, BoxLayout.PAGE_AXIS);
				configPanel.setLayout(bl);
				JScrollPane configPanelScrollPane = new JScrollPane(configPanel);
				mainTabbedPane.addTab("File Browser", null, configPanelScrollPane, "Configure fbx-conv");

				fileChooser = new FileChooserSideBar(this, configPanel);

				configPanel.add(new JSeparator());

				fbxConvLocationBox = new FileChooserFbxConv(this, S_fbxConvLocation, configPanel);



				JPanel flipBase = new JPanel();
				configPanel.add(flipBase);
				flipTextureCoords = new BooleanConfigPanel(this, flipBase, "Flip V Texture Coordinates", B_flipVTextureCoords,true){
					@Override
					protected void onChange() {
						if(fileChooser.isAutomaticPreview())
							previewFile(fileChooser.getSelectedFile(), false);
					}
				};

				maxVertxPanel = new NumberConfigPanel(this, I_maxVertPerMesh, configPanel,
					"Max Verticies per mesh (k)", 32, 1, 50, 1){
					@Override
					protected void onChange() {
						if(fileChooser.isAutomaticPreview())
							previewFile(fileChooser.getSelectedFile(), false);
					}
				};
				maxBonesPanel = new NumberConfigPanel(this, I_maxBonePerNodepart, configPanel,
					"Max Bones per nodepart", 12, 1, 50, 1){
					@Override
					protected void onChange() {
						if(fileChooser.isAutomaticPreview())
							previewFile(fileChooser.getSelectedFile(), false);
					}
				};
				maxBonesWeightsPanel = new NumberConfigPanel(this, I_maxBoneWeightPerVertex, configPanel,
					"Max Bone Weights per vertex", 4, 1, 50, 1){
					@Override
					protected void onChange() {
						if(fileChooser.isAutomaticPreview())
							previewFile(fileChooser.getSelectedFile(), false);
					}
				};
				JPanel packBase = new JPanel();
				configPanel.add(packBase);
				packVertexColors = new BooleanConfigPanel(this, packBase, "Pack vertex colors to one float", B_packVertexColorsToOneFloat,
					false){
					@Override
					protected void onChange() {
						if (fileChooser.isAutomaticPreview())
							previewFile(fileChooser.getSelectedFile(), false);
					}
				};
				outputFileTypeBox = new ComboStringConfigPanel(this, S_outputFileType, configPanel,
					"Output Format", "G3DB", new String[]{"G3DB", "G3DJ"}) {
					@Override
					protected void onChange() {
						fileChooser.refreshConvertButtonText();
					}
				};


			}
			// Viewport Settings
			{

				JPanel viewportSettingsPanel = new JPanel();
				JScrollPane viewportSettingsPanelScrollPane = new JScrollPane(viewportSettingsPanel);
				mainTabbedPane.addTab("Viewport Settings", null, viewportSettingsPanelScrollPane, "Viewport Settings");
				BoxLayout bl = new BoxLayout(viewportSettingsPanel, BoxLayout.PAGE_AXIS);
				viewportSettingsPanel.setLayout(bl);

				JPanel baseEnvPanel = new JPanel();
				viewportSettingsPanel.add(baseEnvPanel);
				environmentLightingBox = new BooleanConfigPanel(this, baseEnvPanel, "Environment Lighting", B_environmentLighting,
					true) {
					@Override
					protected void onChange() {
						modelPreviewApp.environmentLightingEnabled = isSelected();
					}
				};

				JPanel baseBackFacePanel = new JPanel();
				viewportSettingsPanel.add(baseBackFacePanel);
				backFaceCullingBox = new BooleanConfigPanel(this, baseBackFacePanel, "Back Face Culling", B_backFaceCulling,
					true) {
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
				backFaceCullingBox.checkBox.setToolTipText("mat.set(new IntAttribute(IntAttribute.CullFace, 0));");


				JPanel baseAlphaBlending = new JPanel();
				viewportSettingsPanel.add(baseAlphaBlending);
				alphaBlendingBox = new BooleanConfigPanel(this, baseAlphaBlending, "Alpha Blending", B_alphaBlending,
					true) {
					@Override
					protected void onChange() {
						Gdx.app.postRunnable(new Runnable() {
							@Override
							public void run() {
								modelPreviewApp.setAlphaBlending(isSelected());
							}
						});
					}
				};
				alphaBlendingBox.checkBox.setToolTipText("mat.set(new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA));");


				JPanel baseAlphaTest = new JPanel();
				viewportSettingsPanel.add(baseAlphaTest);
				alphaTestBox = new BooleanIntegerConfigPanel(this, baseAlphaTest, "Alpha Test",
					B_alphaTest, false,
					I_alphaTest, 50, 0,100, 1) {
					@Override
					protected void onChange() {
						Gdx.app.postRunnable(new Runnable() {
							@Override
							public void run() {
								if(getBooleanValue())
									modelPreviewApp.setAlphaTest(getIntegerValue()/100f);
								else
									modelPreviewApp.setAlphaTest(-1f);
							}
						});
					}
				};
				alphaTestBox.checkBox.setToolTipText("mat.set(new FloatAttribute(FloatAttribute.AlphaTest, 0.5f));");




				JPanel baseAnimPanel = new JPanel();
				viewportSettingsPanel.add(baseAnimPanel);
				baseAnimPanel.add(new JLabel("Animation: "));
				animComboBox = new JComboBox<Animation>();
				baseAnimPanel.add(animComboBox);


				BasicComboBoxRenderer animComboRenderer = new BasicComboBoxRenderer() {
					@Override
					public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
						if (value == null) {
							setText("<No Animation>");
						} else {
							Animation anim = (Animation) value;
							setText(anim.id + "  -  " + anim.duration);
						}
						return this;
					}
				};

				animComboBox.setRenderer(animComboRenderer);

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
				outputTextPane.setEditable(false);
				outputTextScrollPane = new JScrollPane(outputTextPane);
				mainTabbedPane.addTab("Output Console", null, outputTextScrollPane, "Output");
			}


			// About
			{
				JPanel aboutPanel = new JPanel(new BorderLayout());
				JScrollPane aboutScrollPane = new JScrollPane(aboutPanel);

				String text="libgdx-fbxconv-gui is a lightweight program created by Daniel Strong to help make it easier to get your 3D models ready for LibGDX.";
				text+="\n\nIf you need help or want more information about this software then visit the github page at: http://asneakyfox.github.io/libgdx-fbxconv-gui/";
				JTextArea aboutTextPane = new JTextArea(text);
				aboutTextPane.setLineWrap(true);
				aboutTextPane.setWrapStyleWord(true);
				aboutTextPane.setEditable(false);



				aboutPanel.add(aboutTextPane, BorderLayout.CENTER);


				JButton githubUrlButton = new JButton("View on Github");
				githubUrlButton.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						try {
							Desktop.getDesktop().browse(new URI("http://asneakyfox.github.io/libgdx-fbxconv-gui/"));
						} catch (Throwable t) {
							JOptionPane.showMessageDialog(frame,"I couldnt open your browser while trying to navigate to:\n\nhttp://asneakyfox.github.io/libgdx-fbxconv-gui/");
						}
					}
				});
				aboutPanel.add(githubUrlButton, BorderLayout.SOUTH);

				mainTabbedPane.addTab("About", null, aboutScrollPane, "About");

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



        private void logText(final String text) {
                SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                                if (outputTextPane == null) {
                                        System.out.println(text);
                                        return;
                                }
                                //outputTextPane.setText(outputTextPane.getText() + "\n" + text);
				outputTextPane.setText(text);
                        }
                });
        }

        private void logTextError(Exception e) {

                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                logTextError(sw.toString());
        }

        private void logTextError(Exception e, String hintMessage) {

                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                logTextError(sw.toString()+"\n"+hintMessage);
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
                                mainTabbedPane.setSelectedComponent(outputTextScrollPane);
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
                        "Convert all files" + msgAddition + " to " + dstExtension + " that have the following extension:\n\n WARNING: this will start converting your files and cannot be undone!",
                        "Batch model conversion",
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        options,
                        prefs.get(S_batchConvertFileType, ".fbx"));



                if(srcExtension == null){
                        return;
                }

                prefs.put(S_batchConvertFileType,srcExtension);

                mainTabbedPane.setSelectedComponent(outputTextScrollPane);
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

        protected void previewFile(final File f, final boolean tempPreview) {

                threadPool.submit(new PreviewFileCallable(f, tempPreview));
        }

	private class PreviewFileCallable implements Callable<Void>{

		private final File f;
		private final boolean tempPreview;

		public PreviewFileCallable(File f, boolean tempPreview) {
			this.f = f;
			this.tempPreview = tempPreview;
		}

		@Override
		public Void call() throws Exception {
			Gdx.app.postRunnable(new Runnable() {
				@Override
				public void run() {
					if (tempPreview || f == null)
						modelPreviewApp.showLoadingText("Loading...");
					else
						modelPreviewApp.showLoadingText("Converting\n" + f.getName());
				}
			});


			final File newF = convertFile(f, tempPreview, true);

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

					if (tempPreview && newF != f && newF != null && !newF.isDirectory()) { // only delete newF if it is a temp file that was made in convertFile
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

        private File convertFile(File f, boolean tempPreview, boolean logDetailedOutput) {
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
                String dstExtension = tempPreview || outputFileTypeBox.getValue().equals("G3DJ") ? ".g3dj" : ".g3db";
                String dstPath = targetDir + fbxConvLocationBox.dirSeperator + (tempPreview ? "libgdx-model-viewer.temp" : stripExtension(f.getName())) + dstExtension;
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
                        boolean possibleBadInstallation;
                        try {
                                Process proc = Runtime.getRuntime().exec(fbxConvLocationBox.getAbsolutePath(),null,null);
                                String output = DesktopLauncher.processOutput(proc);
                                possibleBadInstallation = !output.contains("fbx-conv");
                        } catch (IOException ex) {
                                possibleBadInstallation = true;
                        }
                        if(possibleBadInstallation){
                                logTextError(e,"It's possible you either selected the wrong executable file, or you don't have fbx-conv installed correctly.\nIf you're on mac or linux be sure that libfbxsdk.dylib and libfbxsdk.so are in /usr/lib");
                        }else{
                                logTextError(e);
                        }
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
