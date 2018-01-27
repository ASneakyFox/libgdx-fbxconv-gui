package asf.modelpreview.desktop;

import asf.modelpreview.ModelPreviewApp;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl.LwjglAWTCanvas;
import com.badlogic.gdx.graphics.g3d.model.Animation;
import com.badlogic.gdx.utils.Array;

import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
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

	private static final String keyPreferences = "LibGDXModelPreviewUtility";
	private static final String keyI18n = "Labels";

	private final Logger logger;
	public final Preferences prefs;
	private final ExecutorService threadPool;
	private final DisplayLabelManager i18n;
	protected final JFrame frame;

	private final FileConverter fileConverter;
	private int previewCallNum = 0;

	private FileChooserSideBar fileChooser;
	protected JTabbedPane mainTabbedPane;
	private FileChooserFbxConv fbxConvLocationBox;
	private BooleanConfigPanel flipTextureCoords, packVertexColors;
	private NumberConfigPanel maxVertxPanel, maxBonesPanel, maxBonesWeightsPanel;
	protected ComboStringConfigPanel inputFileTypeBox, outputFileTypeBox;
	private BooleanConfigPanel environmentLightingBox, backFaceCullingBox, alphaBlendingBox;
	private BooleanIntegerConfigPanel alphaTestBox;
	private JComboBox<Animation> animComboBox;
	protected JScrollPane outputTextScrollPane;
	protected JTextPane outputTextPane;
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
	//private static final String S_inputFileType = "S_inputFileType";
	private static final String S_outputFileType = "S_outputFileType";
	//private static final String S_batchConvertFileType = "S_batchConvertFileType";
	private static final String B_environmentLighting = "B_environmentLighting";
	private static final String B_backFaceCulling = "B_backFaceCulling";
	private static final String B_alphaBlending = "B_alphaBlending";
	private static final String B_alphaTest = "B_alphaTest";
	private static final String I_alphaTest = "I_alphaTest";


	private DesktopLauncher() {
		logger = new Logger(this);
		prefs = Preferences.userRoot().node(keyPreferences);
		i18n = createI18N();
		threadPool = createThreadPool();
		initAppearance();

		fileConverter = new FileConverter(logger, i18n);

		frame = new JFrame(i18n.get("appTitle"));
		initFrame();
	}

	private static ExecutorService createThreadPool() {
		// TODO: would ideally like to use cached pool, but concurrency errors
		// when changing settings or files very quickly
		//final ExecutorService threadPool = Executors.newCachedThreadPool();

		final ExecutorService threadPool = Executors.newSingleThreadExecutor();

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

	private DisplayLabelManager createI18N() {
		DisplayLabelManager labelManager = new DisplayLabelManager();
		labelManager.loadLabels(keyI18n, "en");
		return labelManager;
	}

	private void initFrame() {

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

		mainTabbedPane.addTab(i18n.get("tabFileBrower"), null, createConfigConvertPanel(), i18n.get("tabFileBrowerTooltip"));
		mainTabbedPane.addTab(i18n.get("tabViewportSettings"), null, createViewportPanel(), i18n.get("tabViewportSettingsTooltip"));
		mainTabbedPane.addTab(i18n.get("tabOutput"), null, createOutputPanel(), i18n.get("tabOutputTooltip"));
		mainTabbedPane.addTab(i18n.get("tabAbout"), null, createAboutPanel(), i18n.get("tabAboutTooltip"));

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
			i18n.get("configPanelFlipTextureCoords"), B_flipVTextureCoords, true) {
			@Override
			protected void onChange() {
				if (fileChooser.isAutomaticPreview())
					fileChooser.displaySelectedFiles(true);
			}
		};

		maxVertxPanel = new NumberConfigPanel(this, I_maxVertPerMesh, configPanel,
			i18n.get("configPanelMaxVertices"), 32767, 1, 32767, 1000) {
			@Override
			protected void onChange() {
				if (fileChooser.isAutomaticPreview())
					fileChooser.displaySelectedFiles(true);
			}
		};
		maxBonesPanel = new NumberConfigPanel(this, I_maxBonePerNodepart, configPanel,
			i18n.get("configPanelMaxBones"), 12, 1, 50, 1) {
			@Override
			protected void onChange() {
				if (fileChooser.isAutomaticPreview())
					fileChooser.displaySelectedFiles(true);
			}
		};
		maxBonesWeightsPanel = new NumberConfigPanel(this, I_maxBoneWeightPerVertex, configPanel,
			i18n.get("configPanelMaxBoneWeights"), 4, 1, 50, 1) {
			@Override
			protected void onChange() {
				if (fileChooser.isAutomaticPreview())
					fileChooser.displaySelectedFiles(true);
			}
		};
		JPanel packBase = new JPanel();
		configPanel.add(packBase);
		packVertexColors = new BooleanConfigPanel(this, packBase,
			i18n.get("configPanelPackVertexColors"), B_packVertexColorsToOneFloat, false) {
			@Override
			protected void onChange() {
				if (fileChooser.isAutomaticPreview())
					fileChooser.displaySelectedFiles(true);
			}
		};

		outputFileTypeBox = new ComboStringConfigPanel(this, S_outputFileType, configPanel,
			i18n.get("configPanelOutputFormat"), "G3DB", new String[]{"G3DB", "G3DJ"}) {
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
		environmentLightingBox = new BooleanConfigPanel(this, baseEnvPanel, i18n.get("panelEnvironmentLighting"), B_environmentLighting, true) {
			@Override
			protected void onChange() {
				modelPreviewApp.environmentLightingEnabled = isSelected();
			}
		};

		JPanel baseBackFacePanel = new JPanel();
		viewportSettingsPanel.add(baseBackFacePanel);
		backFaceCullingBox = new BooleanConfigPanel(this, baseBackFacePanel, i18n.get("panelBackFaceCulling"), B_backFaceCulling, true) {
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
		alphaBlendingBox = new BooleanConfigPanel(this, baseAlphaBlending, i18n.get("panelAlphaBlending"), B_alphaBlending, true) {
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
		alphaTestBox = new BooleanIntegerConfigPanel(this, baseAlphaTest, i18n.get("panelAlphaTest"), B_alphaTest, false, I_alphaTest, 50, 0, 100, 1) {
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
		baseAnimPanel.add(new JLabel(i18n.get("panelAnimation")));
		animComboBox = new JComboBox<Animation>();
		baseAnimPanel.add(animComboBox);

		BasicComboBoxRenderer animComboRenderer = new BasicComboBoxRenderer() {
			@Override
			public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				if (value == null) {
					setText(i18n.get("panelAnimationComboNone"));
				} else {
					Animation anim = (Animation) value;
					setText(anim.id + "  -  " + anim.duration);
				}
				return this;
			}
		};

		animComboBox.setRenderer(animComboRenderer);

		setAnimList(null);

		animComboBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				modelPreviewApp.setAnimation((Animation) animComboBox.getSelectedItem());
			}
		});


		JPanel baseCamPanel = new JPanel();
		viewportSettingsPanel.add(baseCamPanel);
		JButton resetCamButton = new JButton(i18n.get("panelResetCamera"));
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
		JTextArea aboutTextPane = new JTextArea(i18n.get("aboutText"));
		aboutTextPane.setLineWrap(true);
		aboutTextPane.setWrapStyleWord(true);
		aboutTextPane.setEditable(false);

		aboutPanel.add(aboutTextPane, BorderLayout.CENTER);

		JButton githubUrlButton = new JButton(i18n.get("aboutButtonViewOnGitHub"));
		githubUrlButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					Desktop.getDesktop().browse(new URI(i18n.get("aboutButtonViewOnGitHubUrl")));
				} catch (Throwable t) {
					JOptionPane.showMessageDialog(frame, i18n.get("aboutButtonViewOnGitHubError"));
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

	/**
	 * Shows the supplied files in the game window
	 * <p>
	 * does not update the file chooser. preferred approach here
	 * is to change the file chooser selection, which will fire an event
	 * to update the game window.
	 *
	 * @param files       files to show in the game window
	 * @param tempPreview if true, will display file using a temp file location, if false will use a permanent file location (e.g. the conversion function of this program)
	 */
	protected void displayFiles(final File[] files, boolean tempPreview) {
		// TODO: if file contains a directory, handle recursively traveling the folder structure here
		// get3DModelFiles(files)
		threadPool.submit(new PreviewFilesCallable(files, tempPreview, previewCallNum++));
	}

	private List<File> get3DModelFiles(File[] files) {
		List<File> modelFiles = new ArrayList<File>(files.length);

		for (File f : files) {
			if (f.isDirectory()) {
				File[] moreFiles = f.listFiles();
				if (moreFiles != null) {
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
		private final int callNum;

		private PreviewFilesCallable(File[] files, boolean tempPreview, int callNum) {
			this.files = files;
			this.tempPreview = tempPreview;
			this.callNum = callNum;
		}

		@Override
		public Void call() throws Exception {
			Gdx.app.postRunnable(new Runnable() {
				@Override
				public void run() {
					String loadingLabel = i18n.get(tempPreview || files.length == 0 ? "displayFilesLoading" : "displayFilesConverting");
					modelPreviewApp.showLoadingText(loadingLabel);
				}
			});

			final File[] outputFiles = fileConverter.convertFiles(
				fbxConvLocationBox.getAbsolutePath(),
				fbxConvLocationBox.getName(),
				tempPreview || outputFileTypeBox.getValue().equals("G3DJ") ? ".g3dj" : ".g3db",
				flipTextureCoords.isSelected(),
				packVertexColors.isSelected(),
				maxVertxPanel.getValue(), // TODO typo
				maxBonesPanel.getValue(),
				maxBonesWeightsPanel.getValue(), // TODO typo
				files,
				tempPreview,
				callNum,
				true);

			Gdx.app.postRunnable(new Runnable() {
				@Override
				public void run() {
					if (outputFiles.length == 0) {
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
								} catch (Exception ex) {
									//ex.printStackTrace();
									logger.logTextError(i18n.get("displayFilesError", srcF.getName()));
									logger.logTextError(ex);
									modelPreviewApp.previewFile(null);
								}
							}

							// only delete newF if it is a temp file that was made in convertFile
							if (tempPreview && newF != srcF && newF != null && !newF.isDirectory()) {
								try {
									boolean success = newF.delete();
									if (!success) {
										logger.logTextError(i18n.get("diplsayFilesUnableToCleanUpPreview"));
									}
								} catch (Exception ex) {
									logger.logTextError(ex, i18n.get("diplsayFilesUnableToCleanUpPreview"));
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
}
