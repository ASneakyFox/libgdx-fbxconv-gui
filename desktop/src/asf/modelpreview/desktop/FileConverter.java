package asf.modelpreview.desktop;

import org.lwjgl.util.Display;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ResourceBundle;

public class FileConverter {

	protected final String dirSeperator;

	private final Logger logger;
	private final DisplayLabelManager i18n;

	public FileConverter(Logger logger, DisplayLabelManager i18n) {
		this.logger = logger;
		this.i18n = i18n;

		String osName = System.getProperty("os.name").toLowerCase();
		if (osName.contains("win")) {
			dirSeperator = "\\";
		} else if (osName.contains("mac")) {
			dirSeperator = "/";
		} else {
			dirSeperator = "/";
		}
	}


	public static boolean isFbxConvLocationValid(String absoluteFbxConvLocation) {
		return absoluteFbxConvLocation != null;
	}

	public void deleteTemporaryOutputFiles(File[] inputFiles, File[] outputFiles, boolean tempPreview) {
		if(outputFiles != null && tempPreview) {
			for (int i = 0; i < outputFiles.length; i++) {
				File srcF = inputFiles[i];
				File newF = outputFiles[i];

				// only delete newF if it is a temp file that was made in convertFile
				if (newF != srcF && newF != null && !newF.isDirectory()) {
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
	}

	public File[] convertFiles(String absoluteFbxConvLocation,
							   String absoluteFbxConvLocationName,
							   String dstExtension,
							   boolean flipTextureCoords,
							   boolean packVertexColors,
							   int maxVerts,
							   int maxBones,
							   int maxBoneWeights,
							   File[] files, boolean tempPreview, int callNum, boolean logDetailedOutput) {
		if(files == null) return null;
		final File[] outputFiles = new File[files.length];

		for (int i = 0; i < files.length; i++) {
			outputFiles[i] = convertFile(
				absoluteFbxConvLocation,
				absoluteFbxConvLocationName,
				dstExtension,
				flipTextureCoords,
				packVertexColors,
				maxVerts,
				maxBones,
				maxBoneWeights,
				files[i], tempPreview, callNum, i, logDetailedOutput);
		}
		return outputFiles;
	}
	public File convertFile(String absoluteFbxConvLocation,
							String absoluteFbxConvLocationName,
							String dstExtension,
							boolean flipTextureCoords,
							boolean packVertexColors,
							int maxVerts,
							int maxBones,
							int maxBoneWeights,
							File f, boolean tempPreview, int callNum, int previewNum, boolean logDetailedOutput) {
		if (logDetailedOutput) {
			if (f != null && !f.isDirectory()) {
				if (tempPreview)
					logger.logTextClear("Previewing: " + f.getName());
				else
					logger.logTextClear("Converting: " + f.getName());
			} else {
				logger.logTextClear();
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

		if(!isFbxConvLocationValid(absoluteFbxConvLocation)){
			if (logDetailedOutput)
				logger.logTextError(i18n.get("displayFileErrorFbxConvNotConfigured"));
			return null;
		}

		File targetDir = f.getParentFile();

		String dstPath;

		if (tempPreview) {
			dstPath = targetDir + dirSeperator + "libgdx-model-viewer." + callNum + "_" + previewNum + ".temp" + dstExtension;
		} else {
			dstPath = targetDir + dirSeperator + stripExtension(f.getName()) + dstExtension;
		}
		File convertedFile = new File(dstPath);
		try {

			if (logDetailedOutput) {
				//logger.logText("-----------------------------------");
			}

			ProcessBuilder p = new ProcessBuilder(absoluteFbxConvLocation, "-v");
			if (flipTextureCoords) p.command().add("-f");
			if (packVertexColors) p.command().add("-p");
			p.command().add("-m");
			p.command().add(String.valueOf(maxVerts));
			p.command().add("-b");
			p.command().add(String.valueOf(maxBones));
			p.command().add("-w");
			p.command().add(String.valueOf(maxBoneWeights));
			p.command().add(srcPath);
			p.command().add(dstPath);
			if (logDetailedOutput) {
				logger.logText("\n" + shortenCommand(p.command(), absoluteFbxConvLocationName, f.getName(), convertedFile.getName()) + "\n");
			}

			String output = processOutput(p.start());
			if (logDetailedOutput) {
				logger.logText(output);
			}

		} catch (IOException e) {
			boolean possibleBadInstallation;
			try {
				Process proc = Runtime.getRuntime().exec(absoluteFbxConvLocation, null, null);
				String output = processOutput(proc);
				possibleBadInstallation = !output.contains("fbx-conv");
			} catch (IOException ex) {
				possibleBadInstallation = true;
			}
			if (possibleBadInstallation) {
				logger.logTextError(e, i18n.get("displayFilePossibleBadFbxConvInstallation"));
			} else {
				logger.logTextError(e);
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
				logger.logTextError("theres stil more! ");
			}
		} else {
			val = "";
		}
		return val;
	}
}
