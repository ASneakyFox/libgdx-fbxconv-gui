package asf.modelpreview.desktop;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g3d.model.Animation;
import com.badlogic.gdx.utils.Array;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Created by daniel on 1/15/17.
 */
public class ViewportSideBar {

	private final DesktopLauncher desktopLauncher;

	private BooleanConfigPanel environmentLightingBox, backFaceCullingBox, alphaBlendingBox;
	private BooleanIntegerConfigPanel alphaTestBox;
	private JComboBox<Animation> animComboBox;

	public ViewportSideBar(DesktopLauncher desktopLauncher) {
		this.desktopLauncher = desktopLauncher;
	}

	Component buildUi(){
		JPanel viewportSettingsPanel = new JPanel();
		JScrollPane viewportSettingsPanelScrollPane = new JScrollPane(viewportSettingsPanel);

		BoxLayout bl = new BoxLayout(viewportSettingsPanel, BoxLayout.PAGE_AXIS);
		viewportSettingsPanel.setLayout(bl);

		// TODO: should be injecting the keys instead of hard referencing them
		JPanel baseEnvPanel = new JPanel();
		viewportSettingsPanel.add(baseEnvPanel);
		environmentLightingBox = new BooleanConfigPanel(desktopLauncher, baseEnvPanel, "Environment Lighting", DesktopLauncher.B_environmentLighting,
			true) {
			@Override
			protected void onChange() {
				desktopLauncher.modelPreviewApp.environmentLightingEnabled = isSelected();
			}
		};

		JPanel baseBackFacePanel = new JPanel();
		viewportSettingsPanel.add(baseBackFacePanel);
		backFaceCullingBox = new BooleanConfigPanel(desktopLauncher, baseBackFacePanel, "Back Face Culling", DesktopLauncher.B_backFaceCulling,
			true) {
			@Override
			protected void onChange() {
				Gdx.app.postRunnable(new Runnable() {
					@Override
					public void run() {
						desktopLauncher.modelPreviewApp.setBackFaceCulling(isSelected());
					}
				});
			}
		};
		backFaceCullingBox.checkBox.setToolTipText("mat.set(new IntAttribute(IntAttribute.CullFace, 0));");


		JPanel baseAlphaBlending = new JPanel();
		viewportSettingsPanel.add(baseAlphaBlending);
		alphaBlendingBox = new BooleanConfigPanel(desktopLauncher, baseAlphaBlending, "Alpha Blending", DesktopLauncher.B_alphaBlending,
			true) {
			@Override
			protected void onChange() {
				Gdx.app.postRunnable(new Runnable() {
					@Override
					public void run() {
						desktopLauncher.modelPreviewApp.setAlphaBlending(isSelected());
					}
				});
			}
		};
		alphaBlendingBox.checkBox.setToolTipText("mat.set(new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA));");


		JPanel baseAlphaTest = new JPanel();
		viewportSettingsPanel.add(baseAlphaTest);
		alphaTestBox = new BooleanIntegerConfigPanel(desktopLauncher, baseAlphaTest, "Alpha Test",
			DesktopLauncher.B_alphaTest, false,
			DesktopLauncher.I_alphaTest, 50, 0, 100, 1) {
			@Override
			protected void onChange() {
				Gdx.app.postRunnable(new Runnable() {
					@Override
					public void run() {
						if (getBooleanValue())
							desktopLauncher.modelPreviewApp.setAlphaTest(getIntegerValue() / 100f);
						else
							desktopLauncher.modelPreviewApp.setAlphaTest(-1f);
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
				desktopLauncher.modelPreviewApp.setAnimation((Animation) animComboBox.getSelectedItem());
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
						desktopLauncher.modelPreviewApp.resetCam();
					}
				});
			}
		});

		return viewportSettingsPanelScrollPane;
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
}
