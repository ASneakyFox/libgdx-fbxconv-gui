package asf.modelpreview.desktop;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import net.miginfocom.swing.MigLayout;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.concurrent.Callable;

/**
 * Created by Daniel Strong on 10/30/2014.
 */
public class FbxConvSidebar {



	private final DesktopLauncher desktopLauncher;
	private String fbxConvLocation;
	private String fbxConvName;
	/**
	 * the filename of the fbx-conv program that should be looked for (depending on the OS)
	 */
	private final String fbxConvExpectedName;
	final String dirSeperator;

	JPanel basePane;
	JButton downloadFbxConvButton;

	FbxConvSidebar(final DesktopLauncher desktopLauncher) {
		this.desktopLauncher = desktopLauncher;
		String osName = System.getProperty("os.name").toLowerCase();
		if (osName.contains("win")) {
			fbxConvExpectedName = "fbx-conv-win32.exe";
			dirSeperator = "\\";
		} else if (osName.contains("mac")) {
			fbxConvExpectedName = "fbx-conv-mac";
			dirSeperator = "/";
		} else {
			fbxConvExpectedName = "fbx-conv-lin64";
			dirSeperator = "/";
		}

	}

	Component buildUi(){
		final String prefsKey = DesktopLauncher.S_fbxConvLocation;

		fbxConvLocation = desktopLauncher.prefs.get(prefsKey, null);
		if (isValidFbxConvFileLocation(fbxConvLocation)) {
			fbxConvName = new File(fbxConvLocation).getName();
		} else {
			// attempt to detect the fbx conv location
			try {
				String jarPath = FbxConvSidebar.class.getProtectionDomain().getCodeSource().getLocation().getPath();
				fbxConvLocation = Gdx.files.absolute(jarPath).parent().child(fbxConvExpectedName).file().getAbsolutePath();
				if (isValidFbxConvFileLocation(fbxConvLocation)) {
					fbxConvName = new File(fbxConvLocation).getName();
					desktopLauncher.prefs.put(prefsKey, fbxConvLocation);
				} else {
					throw new Exception("invalid fbx conv location: " + fbxConvLocation);
				}

			} catch (Exception e) {
				fbxConvLocation = null;
				fbxConvName = null;
			}
		}


		final JTextField fbxConvLocationField = new JTextField();
		fbxConvLocationField.setText(fbxConvLocation != null ? fbxConvLocation : "");
		fbxConvLocationField.setEditable(false);
		fbxConvLocationField.setMinimumSize(new Dimension(300,0));

		final JFileChooser fbxConvFileChooser = new JFileChooser();
		if (hasValidValue())
			fbxConvFileChooser.setCurrentDirectory(new File(fbxConvLocation));
		fbxConvFileChooser.setDialogTitle("Find the file: " + fbxConvExpectedName);
		fbxConvFileChooser.setDragEnabled(false);
		fbxConvFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		fbxConvFileChooser.setAcceptAllFileFilterUsed(false);
		fbxConvFileChooser.setFileFilter(new FileFilter() {
			@Override
			public boolean accept(File f) {
				return f.isDirectory() || isValidFbxConvFileLocation(f.getAbsolutePath());
			}

			@Override
			public String getDescription() {
				return fbxConvExpectedName;
			}
		});

		JButton browseFbxConvButton = new JButton("Browse...");
		browseFbxConvButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int returnVal = fbxConvFileChooser.showOpenDialog(desktopLauncher.frame);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					File f = fbxConvFileChooser.getSelectedFile();
					String absPath = f.getAbsolutePath();
					fbxConvLocationField.setText(absPath);
					desktopLauncher.prefs.put(prefsKey, absPath);
					fbxConvLocation = absPath;
					fbxConvName = f.getName();
					desktopLauncher.refreshFileBrowserPane();
				}
			}
		});


		downloadFbxConvButton = new JButton("Download fbx-conv");
		downloadFbxConvButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				downloadFbxConvButton.setEnabled(false);
				desktopLauncher.threadPool.submit(new DownloadFbxConvCallable());
			}
		});


		basePane = new JPanel();

		MigLayout layout = new MigLayout("wrap 2");
		basePane.setLayout(layout);

		basePane.add(fbxConvLocationField, "grow");
		basePane.add(browseFbxConvButton);
		basePane.add(downloadFbxConvButton, "span 2");

		JScrollPane mainScrollPane = new JScrollPane(basePane);
		return mainScrollPane;
	}

	private class DownloadFbxConvCallable implements Callable<Void> {
		@Override
		public Void call() throws Exception {
			try{
				//URL zipUrl = new URL("http://libgdx.badlogicgames.com/fbx-conv/fbx-conv.zip");
				//File file = new File("/home/daniel/things/libgdx/fbx-conv-download-test");
				//ZipTool.unpackArchive(zipUrl, file);
				Thread.sleep(5000);
				desktopLauncher.log.text("downloaded fbx-conv");
			} catch(Exception ex){
				desktopLauncher.log.error(ex, "unable to download fbx-conv");
			}
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					downloadFbxConvButton.setEnabled(true);
				}
			});
			return null;
		}
	}

	private boolean isValidFbxConvFileLocation(String absolutePath) {
		if (absolutePath != null) {
			FileHandle absoluteFh = Gdx.files.absolute(absolutePath);
			if (absoluteFh.exists()) {
				String name = absolutePath.toLowerCase();
				return name.endsWith(fbxConvExpectedName);
			}
		}
		return false;
	}

	boolean hasValidValue() {
		return fbxConvLocation != null;
	}

	String getValue() {
		return fbxConvLocation;
	}

	String getValueName() {
		return fbxConvName;
	}

}

