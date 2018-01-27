package asf.modelpreview.desktop;

import asf.modelpreview.ModelPreviewApp;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl.LwjglAWTCanvas;
import com.badlogic.gdx.graphics.g3d.model.Animation;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;

import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
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

	private static final String keyPreferences = "LibGDXModelPreviewUtility";
	private static final String keyI18n = "Labels";

	public final Preferences prefs;
	private final ExecutorService threadPool;
	private final ResourceBundle i18n;
	protected final JFrame frame;

	private FileChooserSideBar fileChooser;
	private JTabbedPane mainTabbedPane;
	private FileChooserFbxConv fbxConvLocationBox;
	private BooleanConfigPanel flipTextureCoords, packVertexColors;
	private NumberConfigPanel maxVertxPanel, maxBonesPanel, maxBonesWeightsPanel;
	protected ComboStringConfigPanel inputFileTypeBox, outputFileTypeBox;
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
	private static final String S_inputFileType = "S_inputFileType";
	private static final String S_outputFileType = "S_outputFileType";
	private static final String S_batchConvertFileType = "S_batchConvertFileType";
	private static final String B_environmentLighting = "B_environmentLighting";
	private static final String B_backFaceCulling = "B_backFaceCulling";
	private static final String B_alphaBlending = "B_alphaBlending";
	private static final String B_alphaTest = "B_alphaTest";
	private static final String I_alphaTest = "I_alphaTest";

	private static ExecutorService createThreadPool() {
		final ExecutorService threadPool = Executors.newCachedThreadPool();
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				threadPool.shutdownNow();
			}
		});
		return threadPool;
	}

	private void initAppearance() {
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
	}

	private ResourceBundle createI18N() {
		Locale currentLocale = new Locale("en");
		return ResourceBundle.getBundle(keyI18n, currentLocale);
	}

	private String getI18nLabel(String key) {
		return i18n.getString(key);
	}

	private DesktopLauncher() {
		prefs = Preferences.userRoot().node(keyPreferences);
		i18n = createI18N();
		threadPool = createThreadPool();
		initAppearance();


		frame = new JFrame(getI18nLabel("appTitle"));
		TransferHandler handler = new TransferHandler() {
			@Override
			public boolean canImport(TransferSupport support) {
				return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
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
				if (data.isEmpty()) {
					return false;
				}

				File[] filesToSelect = new File[data.size()];
				for (int i = 0; i < data.size(); i++) {
					filesToSelect[i] = data.get(i);
				}
				fileChooser.setSelectedFile(filesToSelect);
				return true;
			}

		};
		frame.setTransferHandler(handler);
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		final Container container = frame.getContentPane();

		container.setLayout(new BoxLayout(container, BoxLayout.LINE_AXIS));

		mainTabbedPane = new JTabbedPane();
		mainTabbedPane.setTabPlacement(JTabbedPane.TOP);
		mainTabbedPane.setPreferredSize(new Dimension(525, 1000));
		mainTabbedPane.setMaximumSize(new Dimension(550, 2000));
		container.add(mainTabbedPane);
		container.add(createGameCanvas());

		mainTabbedPane.addTab(getI18nLabel("tabFileBrower"), null, createConfigConvertPanel(), getI18nLabel("tabFileBrowerTooltip"));
		mainTabbedPane.addTab(getI18nLabel("tabViewportSettings"), null, createViewportPanel(), getI18nLabel("tabViewportSettingsTooltip"));
		mainTabbedPane.addTab(getI18nLabel("tabOutput"), null, createOutputPanel(), getI18nLabel("tabOutputTooltip"));
		mainTabbedPane.addTab(getI18nLabel("tabAbout"), null, createAboutPanel(), getI18nLabel("tabAboutTooltip"));

		frame.pack();

		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int height = screenSize.height;
		int width = screenSize.width;
		frame.setSize(Math.round(width * .75f), Math.round(height * .75f));
		frame.setLocationRelativeTo(null);

		frame.setVisible(true);


	}

	private Canvas createGameCanvas() {
		modelPreviewApp = new ModelPreviewApp(this);
		modelPreviewApp.backgroundColor.set(100 / 255f, 149 / 255f, 237 / 255f, 1f);
		LwjglAWTCanvas canvas = new LwjglAWTCanvas(modelPreviewApp);
		return canvas.getCanvas();
	}

	private JComponent createConfigConvertPanel() {
		JPanel configPanel = new JPanel();
		BoxLayout bl = new BoxLayout(configPanel, BoxLayout.PAGE_AXIS);
		configPanel.setLayout(bl);
		JScrollPane configPanelScrollPane = new JScrollPane(configPanel);


		fileChooser = new FileChooserSideBar(this, configPanel);

		configPanel.add(new JSeparator());

		fbxConvLocationBox = new FileChooserFbxConv(this, S_fbxConvLocation, configPanel);


		JPanel flipBase = new JPanel();
		configPanel.add(flipBase);
		flipTextureCoords = new BooleanConfigPanel(this, flipBase,
			getI18nLabel("configPanelFlipTextureCoords"), B_flipVTextureCoords, true) {
			@Override
			protected void onChange() {
				if (fileChooser.isAutomaticPreview())
					fileChooser.displaySelectedFiles(true);
			}
		};

		maxVertxPanel = new NumberConfigPanel(this, I_maxVertPerMesh, configPanel,
			getI18nLabel("configPanelMaxVertices"), 32767, 1, 32767, 1000) {
			@Override
			protected void onChange() {
				if (fileChooser.isAutomaticPreview())
					fileChooser.displaySelectedFiles(true);
			}
		};
		maxBonesPanel = new NumberConfigPanel(this, I_maxBonePerNodepart, configPanel,
			getI18nLabel("configPanelMaxBones"), 12, 1, 50, 1) {
			@Override
			protected void onChange() {
				if (fileChooser.isAutomaticPreview())
					fileChooser.displaySelectedFiles(true);
			}
		};
		maxBonesWeightsPanel = new NumberConfigPanel(this, I_maxBoneWeightPerVertex, configPanel,
			getI18nLabel("configPanelMaxBoneWeights"), 4, 1, 50, 1) {
			@Override
			protected void onChange() {
				if (fileChooser.isAutomaticPreview())
					fileChooser.displaySelectedFiles(true);
			}
		};
		JPanel packBase = new JPanel();
		configPanel.add(packBase);
		packVertexColors = new BooleanConfigPanel(this, packBase,
			getI18nLabel("configPanelPackVertexColors"), B_packVertexColorsToOneFloat, false) {
			@Override
			protected void onChange() {
				if (fileChooser.isAutomaticPreview())
					fileChooser.displaySelectedFiles(true);
			}
		};

		outputFileTypeBox = new ComboStringConfigPanel(this, S_outputFileType, configPanel,
			getI18nLabel("configPanelOutputFormat"), "G3DB", new String[]{"G3DB", "G3DJ"}) {
			@Override
			protected void onChange() {
				fileChooser.refreshConvertButtonText();
			}
		};

		return configPanelScrollPane;
	}

	private JComponent createViewportPanel() {


		JPanel viewportSettingsPanel = new JPanel();
		JScrollPane viewportSettingsPanelScrollPane = new JScrollPane(viewportSettingsPanel);

		BoxLayout bl = new BoxLayout(viewportSettingsPanel, BoxLayout.PAGE_AXIS);
		viewportSettingsPanel.setLayout(bl);

		JPanel baseEnvPanel = new JPanel();
		viewportSettingsPanel.add(baseEnvPanel);
		environmentLightingBox = new BooleanConfigPanel(this, baseEnvPanel, getI18nLabel("panelEnvironmentLighting"), B_environmentLighting, true) {
			@Override
			protected void onChange() {
				modelPreviewApp.environmentLightingEnabled = isSelected();
			}
		};

		JPanel baseBackFacePanel = new JPanel();
		viewportSettingsPanel.add(baseBackFacePanel);
		backFaceCullingBox = new BooleanConfigPanel(this, baseBackFacePanel, getI18nLabel("panelBackFaceCulling"),
			B_backFaceCulling,
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
		alphaBlendingBox = new BooleanConfigPanel(this, baseAlphaBlending, getI18nLabel("panelAlphaBlending"), B_alphaBlending,
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
		alphaTestBox = new BooleanIntegerConfigPanel(this, baseAlphaTest, getI18nLabel("panelAlphaTest"),
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
		baseAnimPanel.add(new JLabel(getI18nLabel("panelAnimation")));
		animComboBox = new JComboBox<Animation>();
		baseAnimPanel.add(animComboBox);


		BasicComboBoxRenderer animComboRenderer = new BasicComboBoxRenderer() {
			@Override
			public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				if (value == null) {
					setText(getI18nLabel("panelAnimationComboNone"));
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
		JButton resetCamButton = new JButton(getI18nLabel("panelResetCamera"));
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

		return viewportSettingsPanelScrollPane;
	}

	private JComponent createOutputPanel() {
		outputTextPane = new JTextPane();
		outputTextPane.setEditable(false);
		outputTextScrollPane = new JScrollPane(outputTextPane);
		return outputTextScrollPane;
	}

	private JComponent createAboutPanel() {
		JPanel aboutPanel = new JPanel(new BorderLayout());
		JScrollPane aboutScrollPane = new JScrollPane(aboutPanel);
		JTextArea aboutTextPane = new JTextArea(getI18nLabel("aboutText"));
		aboutTextPane.setLineWrap(true);
		aboutTextPane.setWrapStyleWord(true);
		aboutTextPane.setEditable(false);

		aboutPanel.add(aboutTextPane, BorderLayout.CENTER);

		JButton githubUrlButton = new JButton(getI18nLabel("aboutButtonViewOnGitHub"));
		githubUrlButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					Desktop.getDesktop().browse(new URI(getI18nLabel("aboutButtonViewOnGitHubUrl")));
				} catch (Throwable t) {
					JOptionPane.showMessageDialog(frame, getI18nLabel("aboutButtonViewOnGitHubError"));
				}
			}
		});
		aboutPanel.add(githubUrlButton, BorderLayout.SOUTH);
		return aboutScrollPane;
	}

	/**
	 * Sets the list of possible animations to be selected for the viewport
	 *
	 * @param animations list of possible animations, null if 3d model does not have any animations.
	 */
	public void setAnimList(Array<Animation> animations) {
		animComboBox.removeAllItems();
		animComboBox.addItem(null);
		if (animations != null) {
			for (Animation animName : animations) {
				animComboBox.addItem(animName);
			}
		}
	}

	private void logTextClear() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				if (outputTextPane == null) {
					System.out.println();
				} else{
					outputTextPane.setText("");
				}
			}
		});
	}

	private void logTextClear(final String text) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				if (outputTextPane == null) {
					System.out.println(text);
				} else {
					outputTextPane.setText(text);
				}

			}
		});
	}

	private void logText(final String text) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				if (outputTextPane == null) {
					System.out.println(text);
				} else {
					outputTextPane.setText(outputTextPane.getText() + "\n" + text);
				}
			}
		});
	}

	private void logTextError(Exception e) {
		logTextError(e, null);
	}

	private void logTextError(Exception e, String hintMessage) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		if(hintMessage != null) {
			logTextError(sw.toString() + "\n" + hintMessage);
		} else {
			logTextError(sw.toString());
		}
	}

	private void logTextError(final String text) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				if (outputTextPane == null) {
					System.err.println(text);
				} else {
					outputTextPane.setText(outputTextPane.getText() + "\n" + text);
					mainTabbedPane.setSelectedComponent(outputTextScrollPane);
				}
			}
		});
	}

	/**
	 *
	 * Shows the supplied files in the game window
	 *
	 * does not update the file chooser. preferred approach here
	 * is to change the file chooser selection, which will fire an event
	 * to update the game window.
	 *
	 * @param files files to show in the game window
	 * @param tempPreview if true, will display file using a temp file location, if false will use a permanent file location (e.g. the conversion function of this program)
	 */
	protected void displayFiles(final File[] files, boolean tempPreview) {
		// TODO: if file contains a directory, handle recursively traveling
		// TODO: the folder structure here
		// get3DModelFiles(files)
		threadPool.submit(new PreviewFilesCallable(files, tempPreview));
	}

	private List<File> get3DModelFiles(File[] files) {
		List<File> modelFiles = new ArrayList<File>(files.length);

		for(File f : files) {
			if(f.isDirectory()) {
				File[] moreFiles = f.listFiles();
				if(moreFiles != null) {
					modelFiles.addAll(get3DModelFiles(moreFiles));
				}
			} else {
				modelFiles.add(f);
			}
		}

		return modelFiles;
	}

	private class PreviewFilesCallable implements Callable<Void> {

		private final File[] files;
		private final boolean tempPreview;

		private PreviewFilesCallable(File[] files, boolean tempPreview) {
			this.files = files;
			this.tempPreview = tempPreview;
		}

		@Override
		public Void call() throws Exception {
			Gdx.app.postRunnable(new Runnable() {
				@Override
				public void run() {
					String loadingLabel = getI18nLabel(tempPreview || files.length == 0 ? "displayFilesLoading": "displayFilesConverting");
					modelPreviewApp.showLoadingText(loadingLabel);
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
					if(outputFiles.length == 0 ) {
						modelPreviewApp.previewFile(null);
					} else {
						for (int i = 0; i < outputFiles.length; i++) {
							File srcF = files[i];
							File newF = outputFiles[i];

							if (newF == null || newF.isDirectory()) {
								modelPreviewApp.previewFile(null);
							} else {
								try {
									modelPreviewApp.previewFile(newF); // TODO: need to have a previewFiles()
								} catch (GdxRuntimeException ex) {
									// TODO: need to attempt to delete file even though there is an error
									logTextError("Error while previewing file: " + srcF.getName()); // TODO label
									logTextError(ex);
									modelPreviewApp.previewFile(null);
								}

							}

							// only delete newF if it is a temp file that was made in convertFile
							if (tempPreview && newF != srcF && newF != null && !newF.isDirectory()) {
								try{
									boolean success = newF.delete();
									if(!success) {
										logTextError(getI18nLabel("diplsayFilesUnableToCleanUpPreview"));
									}
								} catch (Exception ex) {
									logTextError(ex, getI18nLabel("diplsayFilesUnableToCleanUpPreview"));
								}

							}

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
			if (logDetailedOutput) {
				logText("\n" + shortenCommand(p.command(), fbxConvLocationBox.getName(), f.getName(), convertedFile.getName()) + "\n");
			}

			String output = processOutput(p.start());
			if (logDetailedOutput) {
				logText(output);
			}

		} catch (IOException e) {
			boolean possibleBadInstallation;
			try {
				Process proc = Runtime.getRuntime().exec(fbxConvLocationBox.getAbsolutePath(), null, null);
				String output = processOutput(proc);
				possibleBadInstallation = !output.contains("fbx-conv");
			} catch (IOException ex) {
				possibleBadInstallation = true;
			}
			if (possibleBadInstallation) {
				logTextError(e, getI18nLabel("displayFilePossibleBadFbxConvInstallation"));
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
		StringBuilder output = new StringBuilder(shortExecName + " ");
		for (int i = 1; i < command.size() - 2; i++) {
			output.append(command.get(i)).append(" ");
		}

		return output.append(" ").append(shortSrcName).append(" ").append(shortDstName).toString();
	}

	private static String stringArrayToString(String[] stringArray) {
		if (stringArray == null || stringArray.length == 0)
			return "";
		StringBuilder output = new StringBuilder();
		for (String s : stringArray) {
			if (s == null || s.isEmpty()) {
				continue;
			}
			output.append(s).append(" ");
		}

		return output.substring(0, output.length() - 1);
	}

	private String processOutput(Process proc) throws java.io.IOException {
		java.io.InputStream is = proc.getInputStream();
		java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
		String val = "";
		if (s.hasNext()) {
			val = s.next();
			if(s.hasNext()) {
				logTextError("theres stil more! ");
			}
		} else {
			val = "";
		}
		return val;
	}
}
