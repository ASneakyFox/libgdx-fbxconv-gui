package asf.modelpreview.desktop;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Created by daniel on 1/15/17.
 */
public class FbxConvTool {

	private int currentPreviewNum = 0;
	DisplayFileFunction displayFunction;
	boolean logDetailedOutput = true;

	final LogSidebar log; // TODO: use a logger interface instead of direct reference to this gui element
	final DesktopLauncher desktopLauncher; // TODO: shouldnt need reference to DesktopLauncher

	FbxConvTool(LogSidebar log, DesktopLauncher desktopLauncher) {
		this.log = log;
		this.desktopLauncher = desktopLauncher;
	}



	void convertFileRecursive(File[] files, String srcExtension) {
		for (File file : files) {
			convertFileRecursive(file, srcExtension);
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
				File outputFile = convertFile(f);
				if (outputFile != null && outputFile != f) {
					log.text(f.getAbsolutePath() + "--> " + outputFile.getName());
				} else {
					log.error(f.getAbsolutePath() + "--> Error, could not convert!");
				}
			}
		}
	}

	File[] convertFiles(File[] files){
		final File[] outputFiles = new File[files.length];

		currentPreviewNum = 0;
		for (int i = 0; i < files.length; i++) {
			final File newF = convertFile(files[i]);
			outputFiles[i] = newF;
		}

		return outputFiles;
	}

	File convertFile(File f) {
		final boolean previewOnly = displayFunction == DisplayFileFunction.PreviewOnly;
		if (logDetailedOutput) {
			if (f != null && !f.isDirectory()) {
				if (previewOnly)
					log.clear("Previewing: " + f.getName());
				else
					log.clear("Converting: " + f.getName());
			} else {
				log.clear();
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

		if (!desktopLauncher.fbxConvLocationBox.hasValidValue()) {
			if (logDetailedOutput)
				log.error("Can not convert file, fbx-conv location is not yet configured.");
			return null;
		}

		File targetDir = f.getParentFile();
		String dstExtension = previewOnly || desktopLauncher.fileConverterSideBar.outputFileTypeBox.getValue().equals("G3DJ") ? ".g3dj" : ".g3db";
		String dstPath;

		if (previewOnly) {
			dstPath = targetDir + desktopLauncher.fbxConvLocationBox.dirSeperator + "libgdx-model-viewer." + currentPreviewNum + ".temp" + dstExtension;
			currentPreviewNum++;
		} else {
			dstPath = targetDir + desktopLauncher.fbxConvLocationBox.dirSeperator + stripExtension(f.getName()) + dstExtension;
		}
		File convertedFile = new File(dstPath);
		try {

			if (logDetailedOutput) {
				//text("-----------------------------------");
			}

			ProcessBuilder p = new ProcessBuilder(desktopLauncher.fbxConvLocationBox.getValue(), "-v");
			if (desktopLauncher.fileConverterSideBar.flipTextureCoords.isSelected())
				p.command().add("-f");
			if (desktopLauncher.fileConverterSideBar.packVertexColors.isSelected())
				p.command().add("-p");
			p.command().add("-m");
			p.command().add(desktopLauncher.fileConverterSideBar.maxVertxPanel.getString());
			p.command().add("-b");
			p.command().add(desktopLauncher.fileConverterSideBar.maxBonesPanel.getString());
			p.command().add("-w");
			p.command().add(desktopLauncher.fileConverterSideBar.maxBonesWeightsPanel.getString());
			p.command().add(srcPath);
			p.command().add(dstPath);
			if (logDetailedOutput) {
				log.text("\n" + shortenCommand(p.command(), desktopLauncher.fbxConvLocationBox.getValueName(), f.getName(), convertedFile.getName()) + "\n");
			}

			String output = processOutput(p.start());
			if (logDetailedOutput) {
				log.text(output);
			}

		} catch (IOException e) {
			boolean possibleBadInstallation;
			try {
				Process proc = Runtime.getRuntime().exec(desktopLauncher.fbxConvLocationBox.getValue(), null, null);
				String output = processOutput(proc);
				possibleBadInstallation = !output.contains("fbx-conv");
			} catch (IOException ex) {
				possibleBadInstallation = true;
			}
			if (possibleBadInstallation) {
				log.error(e, "It's possible you either selected the wrong executable file, or you don't have fbx-conv installed correctly.\nIf you're on mac or linux be sure that libfbxsdk.dylib and libfbxsdk.so are in /usr/lib");
			} else {
				log.error(e);
			}
			return null;
		}

		return convertedFile;
	}

	// TODO: make private
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

	private static String processOutput(Process proc) throws java.io.IOException {
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
