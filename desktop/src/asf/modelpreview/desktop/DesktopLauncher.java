package asf.modelpreview.desktop;

import asf.modelpreview.ModelPreviewApp;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl.LwjglAWTCanvas;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.graphics.g3d.model.Animation;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.io.File;
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
	final FbxConvTool fbxConvTool;
	final JFrame frame;

	private JTabbedPane mainTabbedPane;
	final FbxConvSidebar fbxConvLocationBox;

	final FileConverterSideBar fileConverterSideBar;
	final ViewportSideBar viewportSideBar;
	final LogSidebar log;
	final AboutSidebar aboutSidebar;
	final ModelPreviewApp modelPreviewApp;


	static final String
		S_folderLocation = "S_folderLocation",
		I_fileFilter = "I_fileFilter",
		B_alwaysConvert = "B_alwaysConvert",
		B_automaticPreview = "B_automaticPreview",
		S_fbxConvLocation = "S_fbxConvLocation",
		B_flipVTextureCoords = "B_flipVTextureCoords",
		B_packVertexColorsToOneFloat = "B_packVertexColorsToOneFloat",
		I_maxVertPerMesh = "I_maxVertPerMesh",
		I_maxBonePerNodepart = "I_maxBonePerNodepart",
		I_maxBoneWeightPerVertex = "I_maxBoneWeightPerVertex",
		S_inputFileType = "S_inputFileType",
		S_outputFileType = "S_outputFileType",
		S_batchConvertFileType = "S_batchConvertFileType",
		B_environmentLighting = "B_environmentLighting",
		B_backFaceCulling = "B_backFaceCulling",
		B_alphaBlending = "B_alphaBlending",
		B_alphaTest = "B_alphaTest",
		I_alphaTest = "I_alphaTest";

	static final String
		KEY_PREFERENCES = "LibGDXModelPreviewUtility",
		LBL_WINDOW_TITLE = "LibGDX Model Preview Utility",
		LBL_FBX_CONV_NOT_CONFIGURED = "Fbx-conv is not configured correctly.\n\nVerify the location is correct on the Config tab.";

	private DesktopLauncher() {
		log = new LogSidebar(this);
		fbxConvTool = new FbxConvTool(log, this);
		fbxConvLocationBox = new FbxConvSidebar(this);
		fileConverterSideBar = new FileConverterSideBar(this);
		viewportSideBar = new ViewportSideBar(this);
		aboutSidebar = new AboutSidebar(this);
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
			log.debugError(e, "unable to set LAF to Nimbus");
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
		modelPreviewApp = new ModelPreviewApp(this); // TODO: need to make sure I am properly access the GDX thread with a callable.
		LwjglApplicationConfiguration canvasConfig = new LwjglApplicationConfiguration();
		LwjglApplicationConfiguration.disableAudio = true;
		canvasConfig.samples = 2;
		canvasConfig.initialBackgroundColor = modelPreviewApp.backgroundColor;
		LwjglAWTCanvas canvas = new LwjglAWTCanvas(modelPreviewApp, canvasConfig);
		container.add(canvas.getCanvas());

		// Left Side Tool Bar
		mainTabbedPane.addTab("Config", null, fbxConvLocationBox.buildUi(), "Configure fbx-conv");
		mainTabbedPane.addTab("File Browser", null, fileConverterSideBar.buildUi(), "Browse and convert files");
		mainTabbedPane.addTab("Viewport Settings", null, viewportSideBar.buildUi(), "Viewport Settings");
		mainTabbedPane.addTab("Output Console", null, log.buildUi(), "Output");
		mainTabbedPane.addTab("About", null, aboutSidebar.buildUI(), "About");

		if(fbxConvLocationBox.hasValidValue())
			fileConverterSideBar.focus();

		frame.pack();

		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int height = screenSize.height;
		int width = screenSize.width;
		frame.setSize(Math.round(width * .75f), Math.round(height * .75f));
		frame.setLocationRelativeTo(null);

		frame.setVisible(true);


	}

	// TODO: remove, suer should access method on sidebar directly
	void refreshFileBrowserPane() {
		fileConverterSideBar.refreshUi();
	}

	// TODO: remove, user should access method on viewport directly
	public void setAnimList(Array<Animation> animations) {
		viewportSideBar.setAnimList(animations);
	}

	/**
	 * request a certain tab to be focused, should be granted as long as there
	 * is not a modal dialog.
	 * @param c
	 */
	void requestFocus(Component c){
		mainTabbedPane.setSelectedComponent(c);
	}

	void showModal(String message){
		JOptionPane.showMessageDialog(frame, message);

	}

	/**
	 *
	 *
	 * JOption prompt to convert all files of a specific type
	 *
	 * TODO: need to merge the recusion logic from this batch converter into the previewFilesCallable
	 *
	 * @param files
	 * @deprecated batch conversions are done by clicking the convert button in the file chooser now.
	 */
	private void convertFilesAsBatch(final List<File> files) {
		if (files.size() == 1 && !files.get(0).isDirectory()) {
			// a single non directory file was chosen, lets just select it
			File[] fs = new File[1];
			fs[0] = files.get(0);
			fileConverterSideBar.fileChooser.setSelectedFile(fs);
			return;
		}

		String[] options = new String[]{".fbx", ".obj", ".dae"};
		String dstExtension = fileConverterSideBar.outputFileTypeBox.getValue().equals("G3DJ") ? ".g3dj" : ".g3db";
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

		log.focus();

		threadPool.submit(new ConvertMultipleFilesCallable(files, srcExtension));

	}

	private class ConvertMultipleFilesCallable implements Callable<Void> {
		final List<File> files;
		final String srcExtension;

		public ConvertMultipleFilesCallable(List<File> files, String srcExtension) {
			this.files = files;
			this.srcExtension = srcExtension;
		}

		@Override
		public Void call() throws Exception {
			log.clear("Batch Convert: " + srcExtension);
			fbxConvTool.logDetailedOutput = false;
			fbxConvTool.displayFunction = DisplayFileFunction.KeepOutput;
			fbxConvTool.convertFileRecursive(files.toArray(new File[files.size()]), srcExtension);
			return null;
		}
	}

	void displayFiles(File[] files, DisplayFileFunction displayFunction){
		threadPool.submit(new PreviewFilesCallable(files, displayFunction));
	}

	private class PreviewFilesCallable implements Callable<Void> {

		private final File[] files;
		private final DisplayFileFunction displayFunction;

		public PreviewFilesCallable(File[] files, DisplayFileFunction displayFunction) {
			this.files = files;
			this.displayFunction = displayFunction;
		}

		@Override
		public Void call() throws Exception {
			Gdx.app.postRunnable(new Runnable() {
				@Override
				public void run() {
					modelPreviewApp.showLoadingText("Loading...");
				}
			});


			fbxConvTool.logDetailedOutput = true;
			fbxConvTool.displayFunction = displayFunction;
			final File[] outputFiles = fbxConvTool.convertFiles(files);

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
								log.error("Error while previewing file: " + srcF.getName());
								log.error(ex);
								modelPreviewApp.previewFile(null);
							}

						}

						// TODO: i got an error before not being able to preview the file because it didnt exist
						// seems to be a concurrency thing, but the code appears to be sequential...

						// only delete newF if it is a temp file that was made in convertFile
						if (displayFunction == DisplayFileFunction.PreviewOnly && newF != srcF && newF != null && !newF.isDirectory()) {
							newF.delete();
						}

					}
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							fileConverterSideBar.fileChooser.refreshFileChooserList();
						}
					});
				}
			});
			return null;
		}
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

	static String arrayToString(Object[] obj){
		String out = "";
		for (Object o : obj) {
			out += o +", ";
		}
		return out;
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
					fileConverterSideBar.fileChooser.setSelectedFile(filesToSelect);
					//convertFilesAsBatch(data);
					return true;
				}else{
					return false;
				}

			}catch(Exception ex){
				log.debugError(ex, "import data error: "+ex.getMessage());
				return false;
			}
		}
	}
}
