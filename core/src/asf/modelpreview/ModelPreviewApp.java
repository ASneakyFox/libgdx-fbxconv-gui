package asf.modelpreview;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGeneratorLoader;
import com.badlogic.gdx.graphics.g2d.freetype.FreetypeFontLoader;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.loader.G3dModelLoader;
import com.badlogic.gdx.graphics.g3d.loader.ObjLoader;
import com.badlogic.gdx.graphics.g3d.model.Animation;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import java.io.File;

public class ModelPreviewApp extends ApplicationAdapter {
	private final DesktopAppResolver desktopLauncher;
	private PerspectiveCamera cam;
	private CameraInputController camController;

	public Environment environment;
	public volatile boolean environmentLightingEnabled = true;
	public final Color backgroundColor = new Color(100 / 255f, 149 / 255f, 237 / 255f, 1f);

	protected ModelBatch modelBatch;

	private Array<Spatial> spatials = new Array<Spatial>(true, 16, Spatial.class);
	//	private Model model;
//	private ModelInstance modelInstance;
//	private AnimationController animController;
	private boolean backFaceCulling = true;
	private boolean alphaBlending = false;
	private float alphaTest = -1;

	private Stage stage;
	private Label label, infoLabel;

	private AssetManager assetManager;
	private G3dModelLoader g3dbModelLoader;
	private G3dModelLoader g3djModelLoader;
	private ObjLoader objLoader;

	public ModelPreviewApp(DesktopAppResolver desktopLauncher) {
		this.desktopLauncher = desktopLauncher;
	}

	public void setBackFaceCulling(boolean backFaceCullinEnabled) {
		backFaceCulling = backFaceCullinEnabled;
		for (Spatial s : spatials) {
			s.setBackFaceCulling(backFaceCulling);
		}
	}

	public void setAlphaBlending(boolean alphaBlendingEnabled) {
		alphaBlending = alphaBlendingEnabled;
		for (Spatial s : spatials) {
			s.setBackFaceCulling(alphaBlending);
		}
	}

	public void setAlphaTest(float alphaTestValue) {
		alphaTest = alphaTestValue;
		for (Spatial s : spatials) {
			s.setAlphaTest(alphaTest);
		}
	}


	public void setBackgroundColor(float r, float g, float b) {
		backgroundColor.set(r, g, b, 1);
		Gdx.gl.glClearColor(backgroundColor.r, backgroundColor.g, backgroundColor.b, backgroundColor.a);
	}

	public void resetBackgroundColor() {
		setBackgroundColor(100 / 255f, 149 / 255f, 237 / 255f);
	}

	public void previewFile(File f) {
		previewFiles(new File[]{f});
	}

	public void previewFiles(File[] files) {

		for (Spatial s : spatials) {
			s.dispose();
		}
		spatials.clear();

		if (files == null || files.length == 0) {
			files = new File[]{null};
		}

		for (File f : files) {
			if (f != null && f.isDirectory()) {
				throw new IllegalArgumentException("provided files must not contain directories");
			}
			Spatial s = new Spatial();
			s.model = loadModel(f);
			spatials.add(s);
			s.init();
		}

		setSpatialLocations();

		Spatial s0 = spatials.get(0); // TODO: consistent scaling for all models
		if (infoLabel != null) {
			if (s0.scalingFactor == 1) {
				infoLabel.setText("");
			} else {
				infoLabel.setText("Scaled to: " + (s0.scalingFactor * 100f) + "%");
			}
		}

		desktopLauncher.setAnimList(s0.model.animations);

		//resetCam();
		if (label != null)
			label.setText("");
	}

	private void setSpatialLocations() {
		int numColumns = MathUtils.ceil((float)Math.sqrt(spatials.size));

		Vector3 locationOffset = new Vector3();
		int currentColumn = 0;
		float largestZ = 0;

		for (Spatial s : spatials) {
			s.setLocation(locationOffset);
			if(currentColumn < numColumns) {
				locationOffset.x += s.dimensions.x * 1.05;
				currentColumn++;
				largestZ = s.dimensions.z > largestZ ? s.dimensions.z : largestZ;
			} else {
				locationOffset.x = 0;
				locationOffset.z += largestZ;
				currentColumn = 0;
				largestZ = 0;
			}
		}


	}

	private Model loadModel(File f) {
		if (f == null) {
			ModelBuilder modelBuilder = new ModelBuilder();
			return modelBuilder.createBox(5f, 5f, 5f,
				new Material(ColorAttribute.createDiffuse(Color.GREEN)),
				VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
		} else {
			String absolutePath = f.getAbsolutePath();

			if (absolutePath.toLowerCase().endsWith("obj")) {
				return objLoader.loadModel(Gdx.files.absolute(absolutePath));
			} else if (absolutePath.toLowerCase().endsWith("g3dj")) {
				return g3djModelLoader.loadModel(Gdx.files.absolute(absolutePath));
			} else {
				return g3dbModelLoader.loadModel(Gdx.files.absolute(absolutePath));
			}
		}
	}

	public void showLoadingText(String text) {
		if (label != null)
			label.setText(text);

	}

	@Override
	public void create() {
		assetManager = new AssetManager();
		FileHandleResolver resolver = new InternalFileHandleResolver();
		assetManager.setLoader(FreeTypeFontGenerator.class, new FreeTypeFontGeneratorLoader(resolver));
		assetManager.setLoader(BitmapFont.class, ".ttf", new FreetypeFontLoader(resolver));

		objLoader = new ObjLoader();
		g3dbModelLoader = new G3dModelLoader(new UBJsonReader());
		g3djModelLoader = new G3dModelLoader(new JsonReader());

		modelBatch = new ModelBatch();

		camController = new CameraInputController(null);
		Gdx.input.setInputProcessor(camController);


		FreetypeFontLoader.FreeTypeFontLoaderParameter fontParameter = new FreetypeFontLoader.FreeTypeFontLoaderParameter();
		fontParameter.fontFileName = "fonts/ariblk.ttf";
		fontParameter.fontParameters.size = Math.round(50f * Gdx.graphics.getDensity());
		//fontParameter.fontParameters.characters="Loading.";
		fontParameter.fontParameters.flip = false;
		assetManager.load("loadingFont.ttf", BitmapFont.class, fontParameter);


		previewFiles(new File[]{null, null});


		environment = new Environment();
		environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
		environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));

		Gdx.gl.glClearColor(backgroundColor.r, backgroundColor.g, backgroundColor.b, backgroundColor.a);

	}

	@Override
	public void render() {
		final float delta = Gdx.graphics.getDeltaTime();
		if (assetManager.update()) {
			if (stage == null && assetManager.isLoaded("loadingFont.ttf")) {
				onFontLoaded(assetManager.get("loadingFont.ttf", BitmapFont.class));
			}
		}

		camController.update();

		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

		modelBatch.begin(cam);

		for (Spatial s : spatials) {
			s.render(delta, this);
		}

		modelBatch.end();

		if (stage != null) {
			stage.draw();
		}
	}

	public void setAnimation(Animation selectedItem) {
		if (spatials.size > 0) {
			Spatial s = spatials.get(0);
			s.setAnimation(selectedItem);
		}
	}

	private void onFontLoaded(BitmapFont font) {
		stage = new Stage(new ScreenViewport());

		label = new Label("", new Label.LabelStyle(font, Color.BLACK));
		label.setFontScale(2);
		Container c1 = new Container<Label>(label);
		c1.setFillParent(true);
		stage.addActor(c1);

		infoLabel = new Label("", new Label.LabelStyle(font, Color.BLACK));
		infoLabel.setFontScale(0.5f);
		Container c2 = new Container<Label>(infoLabel);
		c2.setFillParent(true);
		c2.align(Align.bottomLeft);
		c2.pad(0, 10, 2, 0);
		stage.addActor(c2);
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

	@Override
	public void resize(int width, int height) {
		super.resize(width, height);
		resetCam();
		if (stage != null)
			stage.getViewport().update(width, height, true);
	}

	@Override
	public void dispose() {
		if (modelBatch != null)
			modelBatch.dispose();
		for (Spatial s : spatials)
			s.dispose();
		if (stage != null)
			stage.dispose();
		if (assetManager != null)
			assetManager.dispose();
	}


	public interface DesktopAppResolver {
		public void setAnimList(Array<Animation> animations);
	}
}
