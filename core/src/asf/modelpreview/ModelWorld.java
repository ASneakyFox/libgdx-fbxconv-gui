package asf.modelpreview;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.assets.loaders.resolvers.AbsoluteFileHandleResolver;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGeneratorLoader;
import com.badlogic.gdx.graphics.g2d.freetype.FreetypeFontLoader;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import java.io.File;
import java.util.Iterator;

/**
 * Created by daniel on 1/16/17.
 */
public class ModelWorld implements Disposable{
	public static final Color INITIAL_BACKGROUND_COLOR = new Color(100 / 255f, 149 / 255f, 237 / 255f, 1f);

	final ModelPreviewApp app;
	AssetManager assetManager, absoluteAssetManager;
	private PerspectiveCamera cam;
	CameraInputController camController;
	Stage stage;
	ModelBatch modelBatch;
	public Environment environment;
	public volatile boolean environmentLightingEnabled = true;
	public final Color backgroundColor = new Color(INITIAL_BACKGROUND_COLOR);

	private final Array<Loadable> loadables = new Array<Loadable>(true, 8, Loadable.class);
	private final Array<Loadable> absoluteLoadables = new Array<Loadable>(true, 8, Loadable.class);
	private final Array<View> views = new Array<View>(true, 8, View.class);

	public HudView hudView;
	public ModelView modelView;

	ModelWorld(ModelPreviewApp app){
		this.app = app;
	}

	public void create(){

		FileHandleResolver resolver = new InternalFileHandleResolver();
		assetManager = new AssetManager(resolver, true);
		assetManager.setLoader(FreeTypeFontGenerator.class, new FreeTypeFontGeneratorLoader(resolver));
		assetManager.setLoader(BitmapFont.class, ".ttf", new FreetypeFontLoader(resolver));

		FileHandleResolver absResolver = new AbsoluteFileHandleResolver();
		absoluteAssetManager = new AssetManager(absResolver, true);

		camController = new CameraInputController(null);
		stage = new Stage(new ScreenViewport());
		Gdx.input.setInputProcessor(new InputMultiplexer(stage, camController));

		modelBatch = new ModelBatch();

		addView(hudView = new HudView());
		addView(modelView = new ModelView());

		environment = new Environment();
		environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
		environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));

		Gdx.gl.glClearColor(backgroundColor.r, backgroundColor.g, backgroundColor.b, backgroundColor.a);

		try {
			previewFile(null);
		} catch (GdxRuntimeException e) {
			e.printStackTrace();
		}


	}

	public void resize(int width, int height) {
		resetCam(); // TODO: case for moving camera into a view
		for(View v : views){
			v.resize(width, height);
		}
		stage.getViewport().update(width, height, true);
	}

	public void render(final float delta){

		if(loadables.size > 0) {
			if(assetManager.update()){
				Iterator<Loadable> i = loadables.iterator();
				while(i.hasNext()){
					if(i.next().onLoaded())
						i.remove();
				}
			}
		}

		if(absoluteLoadables.size > 0) {
			if(absoluteAssetManager.update()){
				Iterator<Loadable> i = absoluteLoadables.iterator();
				while(i.hasNext()){
					if(i.next().onLoaded())
						i.remove();
				}
			}
		}



		camController.update();

		for (View view : views) {
			view.update(delta);
		}

		stage.act(delta);

		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
		modelBatch.begin(cam);
		for(View view :views) {
			view.render(delta);
		}
		modelBatch.end();
		stage.draw();

	}

	@Override
	public void dispose() {
		for(View v : views){
			v.dispose();
		}
		modelBatch.dispose();
		stage.dispose();
		assetManager.dispose();
		Gdx.input.setInputProcessor(null);
	}

	public void addView(View view){
		views.add(view);
		view.create(this);
	}

	public void removeView(View view){
		views.removeValue(view, true);
		view.dispose();
	}

	public void addLoadable(Loadable loadable){
		loadables.add(loadable);
	}



	public void removeLoadable(Loadable loadable){
		loadables.removeValue(loadable, true);
		// TODO: need to have a way to handle the fact that assetManager
		// still has the loadables obejcts in its loading buffer.
		// ideally clear out its buffer or something? need to track which
		// files were added
	}

	public void addAbsoluteLoadable(Loadable loadable){
		absoluteLoadables.add(loadable);
	}

	public void setBackgroundColor(float r, float g, float b) {
		backgroundColor.set(r, g, b, 1);
		Gdx.gl.glClearColor(backgroundColor.r, backgroundColor.g, backgroundColor.b, backgroundColor.a);
	}

	public void resetBackgroundColor() {
		setBackgroundColor(100 / 255f, 149 / 255f, 237 / 255f);
	}

	public void previewFile(File f) throws GdxRuntimeException {
		modelView.setModel(f, null);
	}

	public void previewFile(File f, LoadableListener onLoadedCallable) throws GdxRuntimeException {
		modelView.setModel(f, onLoadedCallable);
	}

	public void resetCam() {
		cam = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		cam.position.set(10f, 10f, 10f);
		cam.lookAt(0, 0, 0);
		cam.near = 1f;
		cam.far = 300f;
		cam.update();

		camController.camera = cam;
	}

//	private class InternalInputAdapter extends InputAdapter {
//
//		// The internal input adapter only processes input during the loading phase
//
//		@Override
//		public boolean keyDown(int keycode) {
//			if (keycode == Input.Keys.ESCAPE || keycode == Input.Keys.BACK) {
//				if (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) {
//					dungeonApp.exitApp();
//				}
//				return true;
//			}
//			return false;
//		}
//
//		@Override
//		public boolean keyUp(int keycode) {
//			if (keycode == Input.Keys.ESCAPE || keycode == Input.Keys.BACK) {
//				dungeonApp.setAppPaused(true);
//				return true;
//			} else if (keycode == Input.Keys.F12) {
//				if(dungeonApp.getPlatformActionResolver()!=null)
//					dungeonApp.getPlatformActionResolver().showDebugWindow();
//				return true;
//			}
//			return false;
//		}
//
//	}



	public interface Loadable {
		/**
		 * @return true if the asset that was needed was obtained, false otherwise
		 */
		boolean onLoaded();
	}

	public interface LoadableListener {

		void onLoaded();
	}
}
