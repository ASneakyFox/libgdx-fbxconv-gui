package asf.modelpreview.desktop;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import java.awt.Component;

/**
 * Created by daniel on 1/15/17.
 */
public class FileConverterSideBar {
	private final DesktopLauncher desktopLauncher;

	FileChooserSideBar fileChooser; // TODO: make private, its public methods should move to this class
	JPanel fileBrowserConfiguredPane, fileBrowserNotConfiguredPane;
	JScrollPane fileBrowserScrollPane;

	BooleanConfigPanel flipTextureCoords, packVertexColors;
	NumberConfigPanel maxVertxPanel, maxBonesPanel, maxBonesWeightsPanel;
	ComboStringConfigPanel inputFileTypeBox, outputFileTypeBox;

	public FileConverterSideBar(DesktopLauncher desktopLauncher) {
		this.desktopLauncher = desktopLauncher;
	}

	Component buildUi(){
		fileBrowserNotConfiguredPane = new JPanel();
		BoxLayout bl0 = new BoxLayout(fileBrowserNotConfiguredPane, BoxLayout.PAGE_AXIS);
		fileBrowserNotConfiguredPane.setLayout(bl0);
		fileBrowserNotConfiguredPane.add(new JLabel(DesktopLauncher.LBL_FBX_CONV_NOT_CONFIGURED));


		fileBrowserConfiguredPane = new JPanel();
		BoxLayout bl = new BoxLayout(fileBrowserConfiguredPane, BoxLayout.PAGE_AXIS);
		fileBrowserConfiguredPane.setLayout(bl);


		fileChooser = new FileChooserSideBar(desktopLauncher, fileBrowserConfiguredPane);

		fileBrowserConfiguredPane.add(new JSeparator());

		JPanel flipBase = new JPanel();
		fileBrowserConfiguredPane.add(flipBase);
		// TODO: the preferences keys should be injected.
		flipTextureCoords = new BooleanConfigPanel(desktopLauncher, flipBase, "Flip V Texture Coordinates", DesktopLauncher.B_flipVTextureCoords, true) {
			@Override
			protected void onChange() {
				fileChooser.refreshPreview();
			}
		};

		maxVertxPanel = new NumberConfigPanel(desktopLauncher, DesktopLauncher.I_maxVertPerMesh, fileBrowserConfiguredPane,
			"Max Verticies per mesh (k)", 32, 1, 50, 1) {
			@Override
			protected void onChange() {
				fileChooser.refreshPreview();
			}
		};
		maxBonesPanel = new NumberConfigPanel(desktopLauncher, DesktopLauncher.I_maxBonePerNodepart, fileBrowserConfiguredPane,
			"Max Bones per nodepart", 12, 1, 50, 1) {
			@Override
			protected void onChange() {
				fileChooser.refreshPreview();
			}
		};
		maxBonesWeightsPanel = new NumberConfigPanel(desktopLauncher, DesktopLauncher.I_maxBoneWeightPerVertex, fileBrowserConfiguredPane,
			"Max Bone Weights per vertex", 4, 1, 50, 1) {
			@Override
			protected void onChange() {
				fileChooser.refreshPreview();
			}
		};
		JPanel packBase = new JPanel();
		fileBrowserConfiguredPane.add(packBase);
		packVertexColors = new BooleanConfigPanel(desktopLauncher, packBase, "Pack vertex colors to one float", DesktopLauncher.B_packVertexColorsToOneFloat,
			false) {
			@Override
			protected void onChange() {
				fileChooser.refreshPreview();
			}
		};

		outputFileTypeBox = new ComboStringConfigPanel(desktopLauncher, DesktopLauncher.S_outputFileType, fileBrowserConfiguredPane,
			"Output Format", "G3DB", new String[]{"G3DB", "G3DJ"}) {
			@Override
			protected void onChange() {
				fileChooser.refreshConvertButtonText();
			}
		};


		fileBrowserScrollPane = new JScrollPane(desktopLauncher.fbxConvLocationBox.hasValidValue() ? fileBrowserConfiguredPane : fileBrowserNotConfiguredPane);

		return fileBrowserScrollPane;
	}

	void refreshUi(){
		fileBrowserScrollPane.setViewportView(desktopLauncher.fbxConvLocationBox.hasValidValue() ? fileBrowserConfiguredPane : fileBrowserNotConfiguredPane);
	}

	void focus(){
		desktopLauncher.requestFocus(fileBrowserScrollPane);
	}
}
