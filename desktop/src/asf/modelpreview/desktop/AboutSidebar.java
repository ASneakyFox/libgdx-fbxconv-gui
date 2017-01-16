package asf.modelpreview.desktop;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;

/**
 * Created by daniel on 1/15/17.
 */
public class AboutSidebar {

	private final DesktopLauncher desktopLauncher;

	public AboutSidebar(DesktopLauncher desktopLauncher) {
		this.desktopLauncher = desktopLauncher;
	}

	Component buildUI(){
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
					desktopLauncher.showModal("I couldnt open your browser while trying to navigate to:\n\nhttp://asneakyfox.github.io/libgdx-fbxconv-gui/");
				}
			}
		});
		aboutPanel.add(githubUrlButton, BorderLayout.SOUTH);

		return aboutScrollPane;
	}
}
