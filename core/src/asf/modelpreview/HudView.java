package asf.modelpreview;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.SkinLoader;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreetypeFontLoader;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Align;

/**
 * Created by daniel on 1/16/17.
 */
public class HudView implements View {
	private ModelWorld world;
	private AssetManager assetManager;

	private Stage stage;
	private Skin skin;
	private BitmapFont fontArialBlack;
	private Label label, infoLabel;

	private static final String
		ASSET_FONT_ARIAL_BLK = "loadingFont.ttf",
		ASSET_SKIN = "skins/design3d/skin/uiskin.json",
		ASSET_SKIN_ATLAS = "skins/design3d/skin/uiskin.atlas";

	@Override
	public void create(ModelWorld world) {
		this.world = world;
		this.assetManager = world.assetManager;
		stage = world.stage;

		//skin = new Skin(Gdx.files.internal("Packs/GameSkin.json"));
		//pack = skin.getAtlas();

		assetManager.load(ASSET_SKIN, Skin.class, new SkinLoader.SkinParameter(ASSET_SKIN_ATLAS));

		FreetypeFontLoader.FreeTypeFontLoaderParameter fontParameter = new FreetypeFontLoader.FreeTypeFontLoaderParameter();
		fontParameter.fontFileName = "fonts/ariblk.ttf";
		fontParameter.fontParameters.size = Math.round(50f * Gdx.graphics.getDensity());
		//fontParameter.fontParameters.characters="Loading.";
		fontParameter.fontParameters.flip = false;
		assetManager.load(ASSET_FONT_ARIAL_BLK, BitmapFont.class, fontParameter);

		world.addLoadable(new HudLoadable());
	}

	private class HudLoadable implements ModelWorld.Loadable {

		@Override
		public boolean onLoaded() {
			if (assetManager.isLoaded(ASSET_FONT_ARIAL_BLK) && assetManager.isLoaded(ASSET_SKIN)) {
				fontArialBlack = assetManager.get(ASSET_FONT_ARIAL_BLK, BitmapFont.class);
				skin = assetManager.get(ASSET_SKIN, Skin.class);

				label = new Label("", new Label.LabelStyle(fontArialBlack, Color.BLACK));
				label.setFontScale(2);
				Container c1 = new Container<Label>(label);
				c1.setFillParent(true);
				stage.addActor(c1);

				infoLabel = new Label("", new Label.LabelStyle(fontArialBlack, Color.BLACK));
				infoLabel.setFontScale(0.5f);
				Container c2 = new Container<Label>(infoLabel);
				c2.setFillParent(true);
				c2.align(Align.bottomLeft);
				c2.pad(0, 10, 2, 0);
				stage.addActor(c2);

				return true;
			}else{
				return false;
			}
		}
	}

	public void resize(int width, int height) {


	}
	@Override
	public void update(float delta) {

	}

	@Override
	public void render(float delta) {

	}

	@Override
	public void dispose() {

	}

	public void showLoadingText(String text) {
		if (label != null)
			label.setText(text);

	}


}
