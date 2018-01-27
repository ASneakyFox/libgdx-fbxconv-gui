package asf.modelpreview.desktop;

import asf.modelpreview.ModelPreviewApp;

import javax.swing.*;
import java.io.PrintWriter;
import java.io.StringWriter;

public class Logger {

	private final DesktopLauncher desktopLauncher;

	public Logger(DesktopLauncher desktopLauncher) {
		this.desktopLauncher = desktopLauncher;
	}

	public void logTextClear() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				if (desktopLauncher.outputTextPane == null) {
					System.out.println();
				} else{
					desktopLauncher.outputTextPane.setText("");
				}
			}
		});
	}

	public void logTextClear(final String text) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				if (desktopLauncher.outputTextPane == null) {
					System.out.println(text);
				} else {
					desktopLauncher.outputTextPane.setText(text);
				}

			}
		});
	}

	public void logText(final String text) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				if (desktopLauncher.outputTextPane == null) {
					System.out.println(text);
				} else {
					// TODO thread safety issue here with the get
					desktopLauncher.outputTextPane.setText(desktopLauncher.outputTextPane.getText() + "\n" + text);
				}
			}
		});
	}

	public void logTextError(Exception e) {
		logTextError(e, null);
	}

	public void logTextError(Exception e, String hintMessage) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		if(hintMessage != null) {
			logTextError(sw.toString() + "\n" + hintMessage);
		} else {
			logTextError(sw.toString());
		}
	}

	public void logTextError(final String text) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				if (desktopLauncher.outputTextPane == null || desktopLauncher.mainTabbedPane == null || desktopLauncher.outputTextScrollPane == null) {
					System.err.println(text);
				} else {
					// TODO: thread safety issue here with the get
					desktopLauncher.outputTextPane.setText(desktopLauncher.outputTextPane.getText() + "\n" + text);
					desktopLauncher.mainTabbedPane.setSelectedComponent(desktopLauncher.outputTextScrollPane);
				}
			}
		});
	}
}
