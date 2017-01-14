package asf.modelpreview.desktop;

import asf.modelpreview.ModelPreviewApp;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl.LwjglAWTCanvas;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
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

	final Preferences prefs;
	final ExecutorService threadPool;
	final JFrame frame;

	private FileChooserSideBar fileChooser;
	private JTabbedPane mainTabbedPane;
	JScrollPane fileBrowserScrollPane;
	JPanel fileBrowserConfiguredPane, fileBrowserNotConfiguredPane;
	private FileChooserFbxConv fbxConvLocationBox;
	private BooleanConfigPanel flipTextureCoords, packVertexColors;
	private NumberConfigPanel maxVertxPanel, maxBonesPanel, maxBonesWeightsPanel;
	ComboStringConfigPanel inputFileTypeBox, outputFileTypeBox;
	private BooleanConfigPanel environmentLightingBox, backFaceCullingBox, alphaBlendingBox;
	private BooleanIntegerConfigPanel alphaTestBox;
	private JComboBox<Animation> animComboBox;
	private JScrollPane outputTextScrollPane;
	private JTextPane outputTextPane;
	private ModelPreviewApp modelPreviewApp;


	static final String S_folderLocation = "S_folderLocation";
	static final String I_fileFilter = "I_fileFilter";
	//static final String B_alwaysConvert = "B_alwaysConvert";
	static final String B_automaticPreview = "B_automaticPreview";
	private static final String S_fbxConvLocation = "S_fbxConvLocation";
	private static final String B_flipVTextureCoords = "B_flipVTextureCoords";
	private static final String B_packVertexColorsToOneFloat = "B_packVertexColorsToOneFloat";
	private static final String I_maxVertPerMesh = "I_maxVertPerMesh";
	private static final String I_maxBonePerNodepart = "I_maxBonePerNodepart";
	private static final String I_maxBoneWeightPerVertex = "I_maxBoneWeightPerVertex";
	private static final String S_inputFileType = "S_inputFileType";
	private static final String S_outputFileType = "S_outputFileType";
	private static final String S_batchConvertFileType = "S_batchConvertFileType";
	private static final String B_environmentLighting = "B_environmentLighting";
	private static final String B_backFaceCulling = "B_backFaceCulling";
	private static final String B_alphaBlending = "B_alphaBlending";
	private static final String B_alphaTest = "B_alphaTest";
	private static final String I_alphaTest = "I_alphaTest";

	private static final String
		KEY_PREFERENCES = "LibGDXModelPreviewUtility2",
		LBL_WINDOW_TITLE = "LibGDX Model Preview Utility",
		LBL_FBX_CONV_NOT_CONFIGURED = "Fbx-conv is not configured correctly.\n\nVerify the location is correct on the Config tab.";

	private DesktopLauncher() {
		prefs = Preferences.userRoot().node(KEY_PREFERENCES);

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

		frame = new JFrame(LBL_WINDOW_TITLE);
		frame.setTransferHandler(new DnDTransferHandler());
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		final Container container = frame.getContentPane();
		container.setLayout(new BoxLayout(container, BoxLayout.LINE_AXIS));

		mainTabbedPane = new JTabbedPane();
		mainTabbedPane.setTabPlacement(JTabbedPane.TOP);
		mainTabbedPane.setPreferredSize(new Dimension(525, 1000));
		mainTabbedPane.setMaximumSize(new Dimension(550, 2000));
		container.add(mainTabbedPane);

		// libgdx canvas
		{
			modelPreviewApp = new ModelPreviewApp(this);
			LwjglApplicationConfiguration canvasConfig = new LwjglApplicationConfiguration();
			LwjglApplicationConfiguration.disableAudio = true;
			canvasConfig.samples = 2;
			canvasConfig.initialBackgroundColor = modelPreviewApp.backgroundColor;
			LwjglAWTCanvas canvas = new LwjglAWTCanvas(modelPreviewApp, canvasConfig);
			container.add(canvas.getCanvas());
		}

		// Left Side Tool Bar
		{

			// fbx-conv Configuration
			{
				fbxConvLocationBox = new FileChooserFbxConv(this, S_fbxConvLocation);
				JScrollPane mainScrollPane = new JScrollPane(fbxConvLocationBox.basePane);
				mainTabbedPane.addTab("Config", null, mainScrollPane, "Configure fbx-conv");
			}

			// File Browser
			{
				fileBrowserNotConfiguredPane = new JPanel();
				BoxLayout bl0 = new BoxLayout(fileBrowserNotConfiguredPane, BoxLayout.PAGE_AXIS);
				fileBrowserNotConfiguredPane.setLayout(bl0);
				fileBrowserNotConfiguredPane.add(new JLabel(LBL_FBX_CONV_NOT_CONFIGURED));


				fileBrowserConfiguredPane = new JPanel();
				BoxLayout bl = new BoxLayout(fileBrowserConfiguredPane, BoxLayout.PAGE_AXIS);
				fileBrowserConfiguredPane.setLayout(bl);


				fileChooser = new FileChooserSideBar(this, fileBrowserConfiguredPane);

				fileBrowserConfiguredPane.add(new JSeparator());

				JPanel flipBase = new JPanel();
				fileBrowserConfiguredPane.add(flipBase);
				flipTextureCoords = new BooleanConfigPanel(this, flipBase, "Flip V Texture Coordinates", B_flipVTextureCoords, true) {
					@Override
					protected void onChange() {
						if (fileChooser.isAutomaticPreview())
							displaySelectedFiles(true);
					}
				};

				maxVertxPanel = new NumberConfigPanel(this, I_maxVertPerMesh, fileBrowserConfiguredPane,
					"Max Verticies per mesh (k)", 32, 1, 50, 1) {
					@Override
					protected void onChange() {
						if (fileChooser.isAutomaticPreview())
							displaySelectedFiles(true);
					}
				};
				maxBonesPanel = new NumberConfigPanel(this, I_maxBonePerNodepart, fileBrowserConfiguredPane,
					"Max Bones per nodepart", 12, 1, 50, 1) {
					@Override
					protected void onChange() {
						if (fileChooser.isAutomaticPreview())
							displaySelectedFiles(true);
					}
				};
				maxBonesWeightsPanel = new NumberConfigPanel(this, I_maxBoneWeightPerVertex, fileBrowserConfiguredPane,
					"Max Bone Weights per vertex", 4, 1, 50, 1) {
					@Override
					protected void onChange() {
						if (fileChooser.isAutomaticPreview())
							displaySelectedFiles(true);
					}
				};
				JPanel packBase = new JPanel();
				fileBrowserConfiguredPane.add(packBase);
				packVertexColors = new BooleanConfigPanel(this, packBase, "Pack vertex colors to one float", B_packVertexColorsToOneFloat,
					false) {
					@Override
					protected void onChange() {
						if (fileChooser.isAutomaticPreview())
							displaySelectedFiles(true);
					}
				};

				outputFileTypeBox = new ComboStringConfigPanel(this, S_outputFileType, fileBrowserConfiguredPane,
					"Output Format", "G3DB", new String[]{"G3DB", "G3DJ"}) {
					@Override
					protected void onChange() {
						fileChooser.refreshConvertButtonText();
					}
				};


				fileBrowserScrollPane = new JScrollPane(fbxConvLocationBox.hasValidValue() ? fileBrowserConfiguredPane : fileBrowserNotConfiguredPane);
				mainTabbedPane.addTab("File Browser", null, fileBrowserScrollPane, "Browse and convert files");
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
					I_alphaTest, 50, 0, 100, 1) {
					@Override
					protected void onChange() {
						Gdx.app.postRunnable(new Runnable() {
							@Override
							public void run() {
								if (getBooleanValue())
									modelPreviewApp.setAlphaTest(getIntegerValue() / 100f);
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
						modelPreviewApp.setAnimation((Animation) animComboBox.getSelectedItem());
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

				String text = "libgdx-fbxconv-gui is a lightweight program created by Daniel Strong to help make it easier to get your 3D models ready for LibGDX.";
				text += "\n\nIf you need help or want more information about this software then visit the github page at: http://asneakyfox.github.io/libgdx-fbxconv-gui/";
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
							JOptionPane.showMessageDialog(frame, "I couldnt open your browser while trying to navigate to:\n\nhttp://asneakyfox.github.io/libgdx-fbxconv-gui/");
						}
					}
				});
				aboutPanel.add(githubUrlButton, BorderLayout.SOUTH);

				mainTabbedPane.addTab("About", null, aboutScrollPane, "About");

			}
		}

		if(fbxConvLocationBox.hasValidValue())
			mainTabbedPane.setSelectedComponent(fileBrowserScrollPane);

		frame.pack();

		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int height = screenSize.height;
		int width = screenSize.width;
		frame.setSize(Math.round(width * .75f), Math.round(height * .75f));
		frame.setLocationRelativeTo(null);

		frame.setVisible(true);


	}

	void refreshFileBrowserPane() {
		fileBrowserScrollPane.setViewportView(fbxConvLocationBox.hasValidValue() ? fileBrowserConfiguredPane : fileBrowserNotConfiguredPane);
	}

	public void setAnimList(Array<Animation> animations) {
		animComboBox.removeAllItems();
		animComboBox.addItem(null);
		if (animations == null)
			return;

		for (Animation animName : animations) {
			animComboBox.addItem(animName);
		}
	}


	void logTextClear() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				if (outputTextPane == null) {
					System.out.println();
					return;
				}
				outputTextPane.setText("");
			}
		});
	}

	void logTextClear(final String text) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				if (outputTextPane == null) {
					System.out.println(text);
					return;
				}
				outputTextPane.setText(text);
			}
		});
	}

	void logText(final String text) {
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

	void logTextError(Throwable e) {

		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		logTextError(sw.toString());
	}

	void logTextError(Throwable e, String hintMessage) {

		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		logTextError(sw.toString() + "\n" + hintMessage);
	}

	void logTextError(final String text) {
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

	/**
	 * @param files
	 * @deprecated batch conversions are done by clicking the convert button in the file chooser now.
	 */
	private void convertFilesAsBatch(final List<File> files) {
		if (files.size() == 1 && !files.get(0).isDirectory()) {
			// a single non directory file was chosen, lets just select it
			File[] fs = new File[1];
			fs[0] = files.get(0);
			fileChooser.setSelectedFile(fs);
			return;
		}

		String[] options = new String[]{".fbx", ".obj", ".dae"};
		String dstExtension = outputFileTypeBox.getValue().equals("G3DJ") ? ".g3dj" : ".g3db";
		String msgAddition = files.size() == 1 && files.get(0).isDirectory() ? " in " + files.get(0).getName() : "";
		final String srcExtension = (String) JOptionPane.showInputDialog(
			frame,
			"Convert all files" + msgAddition + " to " + dstExtension + " that have the following extension:\n\n WARNING: this will start converting your files and cannot be undone!",
			"Batch model conversion",
			JOptionPane.QUESTION_MESSAGE,
			null,
			options,
			prefs.get(S_batchConvertFileType, ".fbx"));


		if (srcExtension == null) {
			return;
		}

		prefs.put(S_batchConvertFileType, srcExtension);

		mainTabbedPane.setSelectedComponent(outputTextScrollPane);


		threadPool.submit(new ConvertMultipleFilesCallable(files, srcExtension, dstExtension));

	}

	private class ConvertMultipleFilesCallable implements Callable<Void> {
		final List<File> files;
		final String srcExtension;
		final String dstExtension;

		public ConvertMultipleFilesCallable(List<File> files, String srcExtension, String dstExtension) {
			this.files = files;
			this.srcExtension = srcExtension;
			this.dstExtension = dstExtension;
		}

		@Override
		public Void call() throws Exception {
			logTextClear("Batch Convert: " + srcExtension + " -> " + dstExtension);
			for (File file : files) {
				convertFileRecursive(file, srcExtension);
			}
			return null;
		}
	}

	private void convertFileRecursive(File f, String srcExtension) {

		if (f.isDirectory()) {
			File[] files = f.listFiles();
			for (File file : files) {
				convertFileRecursive(file, srcExtension);
			}
		} else {
			if (f.getName().toLowerCase().endsWith(srcExtension)) {
				File outputFile = convertFile(f, false, false);
				if (outputFile != null && outputFile != f) {
					logText(f.getAbsolutePath() + "--> " + outputFile.getName());
				} else {
					logTextError(f.getAbsolutePath() + "--> Error, could not convert!");
				}
			}
		}
	}

	/**
	 * shows the files chosen that are selected within the FIleChooser in the 3d Window
	 *
	 * @param tempPreview if true the output files are temporary and will be deleted, if false they will be kept (ie for the conversion function of the program)
	 */
	protected void displaySelectedFiles(boolean tempPreview) {
		System.out.println("displaySelectedFiles("+tempPreview+")");
		if (fileChooser == null) {
			System.err.println("fileChooser was null");
			return;
		}
		File[] files = fileChooser.getSelectedFilesToConvert();
		if(files.length == 0){
			System.out.println("files.length == 0");
		}else{
			threadPool.submit(new PreviewFilesCallable(files, tempPreview));

		}
	}

	private class PreviewFilesCallable implements Callable<Void> {

		private final File[] files;
		private final boolean tempPreview;

		public PreviewFilesCallable(File[] files, boolean tempPreview) {
			this.files = files;
			this.tempPreview = tempPreview;
		}

		@Override
		public Void call() throws Exception {
			Gdx.app.postRunnable(new Runnable() {
				@Override
				public void run() {
					modelPreviewApp.showLoadingText("Loading...");
				}
			});


			final File[] outputFiles = new File[files.length];

			currentPreviewNum = 0;
			for (int i = 0; i < files.length; i++) {
				final File newF = convertFile(files[i], tempPreview, true);
				outputFiles[i] = newF;
			}

			Gdx.app.postRunnable(new Runnable() {
				@Override
				public void run() {
					for (int i = 0; i < outputFiles.length; i++) {
						File srcF = files[i];
						File newF = outputFiles[i];

						if (newF == null || newF.isDirectory()) {
							modelPreviewApp.previewFile(null);
						} else {
							try {
								modelPreviewApp.previewFile(newF);
							} catch (GdxRuntimeException ex) {
								logTextError("Error while previewing file: " + srcF.getName());
								logTextError(ex);
								modelPreviewApp.previewFile(null);
							}

						}

						// only delete newF if it is a temp file that was made in convertFile
						if (tempPreview && newF != srcF && newF != null && !newF.isDirectory()) {
							newF.delete();
						}

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

	/**
	 * @param f
	 * @param tempPreview
	 * @deprecated use displaySelectedFiles instead
	 */
	protected void previewFile(final File f, final boolean tempPreview) {

		threadPool.submit(new PreviewFileCallable(f, tempPreview));
	}

	@Deprecated
	private class PreviewFileCallable implements Callable<Void> {

		private final File f;
		private final boolean tempPreview;

		PreviewFileCallable(File f, boolean tempPreview) {
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


			currentPreviewNum = 0;
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


	private int currentPreviewNum = 0;

	private File convertFile(File f, boolean tempPreview, boolean logDetailedOutput) {
		if (logDetailedOutput) {
			if (f != null && !f.isDirectory()) {
				if (tempPreview)
					logTextClear("Previewing: " + f.getName());
				else
					logTextClear("Converting: " + f.getName());
			} else {
				logTextClear();
			}
		}


		if (f == null || f.isDirectory()) {
			return null; // not a model file
		}
		String srcPath = f.getAbsolutePath();
		String srcLower = srcPath.toLowerCase();
		if (srcLower.endsWith(".g3db") || srcLower.endsWith(".g3dj")) {
			return f; // Already in desirable format, return the same file
		}

		if (!fbxConvLocationBox.hasValidValue()) {
			if (logDetailedOutput)
				logTextError("Can not convert file, fbx-conv location is not yet configured.");
			return null;
		}

		File targetDir = f.getParentFile();
		String dstExtension = tempPreview || outputFileTypeBox.getValue().equals("G3DJ") ? ".g3dj" : ".g3db";
		String dstPath;

		if (tempPreview) {
			dstPath = targetDir + fbxConvLocationBox.dirSeperator + "libgdx-model-viewer." + currentPreviewNum + ".temp" + dstExtension;
			currentPreviewNum++;
		} else {
			dstPath = targetDir + fbxConvLocationBox.dirSeperator + stripExtension(f.getName()) + dstExtension;
		}
		File convertedFile = new File(dstPath);
		try {

			if (logDetailedOutput) {
				//logText("-----------------------------------");
			}

			ProcessBuilder p = new ProcessBuilder(fbxConvLocationBox.getValue(), "-v");
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
			if (logDetailedOutput) {
				logText("\n" + shortenCommand(p.command(), fbxConvLocationBox.getValueName(), f.getName(), convertedFile.getName()) + "\n");
			}

			String output = processOutput(p.start());
			if (logDetailedOutput) {
				logText(output);
			}

		} catch (IOException e) {
			boolean possibleBadInstallation;
			try {
				Process proc = Runtime.getRuntime().exec(fbxConvLocationBox.getValue(), null, null);
				String output = DesktopLauncher.processOutput(proc);
				possibleBadInstallation = !output.contains("fbx-conv");
			} catch (IOException ex) {
				possibleBadInstallation = true;
			}
			if (possibleBadInstallation) {
				logTextError(e, "It's possible you either selected the wrong executable file, or you don't have fbx-conv installed correctly.\nIf you're on mac or linux be sure that libfbxsdk.dylib and libfbxsdk.so are in /usr/lib");
			} else {
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

	private class DnDTransferHandler extends TransferHandler {
		// http://stackoverflow.com/questions/13597233/how-to-drag-and-drop-files-from-a-directory-in-java
		// http://stackoverflow.com/questions/9192371/dragn-drop-files-from-the-os-to-java-application-swing
		@Override
		public boolean canImport(TransferSupport support) {
			return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor); // interesting ideas: http://stackoverflow.com/questions/16592166/is-the-transfer-data-for-transferables-from-outside-the-jvm-null-by-default-when
		}

		@Override
		public boolean importData(TransferSupport support) {
			try{
				List<File> data;
				Object dataObject = support.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
				if(dataObject instanceof List){
					data = (List<File>) dataObject;
					if (data.isEmpty()) {
						return false;
					}
					File[] filesToSelect = new File[data.size()];
					for (int i = 0; i < data.size(); i++) {
						filesToSelect[i] = data.get(i);
					}
					fileChooser.setSelectedFile(filesToSelect);
					//convertFilesAsBatch(data);
					return true;
				}else{
					return false;
				}

			}catch(Exception ex){
				System.err.println("import data error: "+ex.getMessage());
				ex.printStackTrace();
				return false;
			}
		}
	}
}
