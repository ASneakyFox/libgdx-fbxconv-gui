package asf.modelpreview;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGeneratorLoader;
import com.badlogic.gdx.graphics.g2d.freetype.FreetypeFontLoader;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.loader.G3dModelLoader;
import com.badlogic.gdx.graphics.g3d.loader.ObjLoader;
import com.badlogic.gdx.graphics.g3d.model.Animation;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.UBJsonReader;

import java.io.File;

public class ModelPreviewApp extends ApplicationAdapter {
        private final DesktopAppResolver desktopLauncher;
        private PerspectiveCamera cam;
        private CameraInputController camController;

        public Environment environment;
        public volatile boolean environmentLightingEnabled = true;
        public final Color backgroundColor = new Color(0,0,0,1);



        private ModelBatch modelBatch;


        private Model model;
        private ModelInstance instance;
        private AnimationController animController;

        private Stage stage;
        private Table table;
        private Label label;


        private AssetManager assetManager;
        private G3dModelLoader g3dbModelLoader;
        private G3dModelLoader g3djModelLoader;
        private ObjLoader objLoader;

        public ModelPreviewApp(DesktopAppResolver desktopLauncher) {
                this.desktopLauncher = desktopLauncher;
        }

        public void previewFile(File f) throws GdxRuntimeException {
                if(model != null){
                        model.dispose();
                        model = null;
                        animController = null;
                }

                if(f == null){
                        ModelBuilder modelBuilder = new ModelBuilder();
                        model = modelBuilder.createBox(5f, 5f, 5f,
                                new Material(ColorAttribute.createDiffuse(Color.GREEN)),
                                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);

                }else{
                        String absolutePath = f.getAbsolutePath();

                        if(absolutePath.toLowerCase().endsWith("obj")){
                                model = objLoader.loadModel(Gdx.files.absolute(absolutePath));
                        }else if(absolutePath.toLowerCase().endsWith("g3dj")){
                                model = g3djModelLoader.loadModel(Gdx.files.absolute(absolutePath));
                        }else{
                                model = g3dbModelLoader.loadModel(Gdx.files.absolute(absolutePath));
                        }


                }


                onModelLoaded(model);


        }

        public void showLoadingText(String text){
                if(label != null)
                label.setText(text);

        }


	@Override
	public void create () {
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
                fontParameter.fontParameters.size=100;
                //fontParameter.fontParameters.characters="Loading.";
                fontParameter.fontParameters.flip = false;
                assetManager.load("loadingFont.ttf", BitmapFont.class,fontParameter);


                try{
                        previewFile(null);
                }catch(GdxRuntimeException e){
                        e.printStackTrace();
                }


                environment = new Environment();
                environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
                environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));


	}

	@Override
	public void render () {
                if(assetManager.update()){
                        if(stage == null && assetManager.isLoaded("loadingFont.ttf")){
                                onFontLoaded(assetManager.get("loadingFont.ttf", BitmapFont.class));
                        }
                }

                camController.update();

                Gdx.gl.glClearColor(backgroundColor.r,backgroundColor.g,backgroundColor.b,backgroundColor.a);
                Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
                Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);


                if(instance != null){
                        if(animController != null){
                                animController.update(Gdx.graphics.getDeltaTime());
                        }
                        modelBatch.begin(cam);
                        modelBatch.render(instance, environmentLightingEnabled ? environment : null);
                        modelBatch.end();
                }

                if(stage != null)
                        stage.draw();


	}

        public void setAnimation(Animation selectedItem) {
                if(animController == null)
                        return;

                if(selectedItem == null){
                        animController.setAnimation(null,-1);
                }else{
                        animController.setAnimation(selectedItem.id,-1);
                }


        }

        private void onModelLoaded(Model model){
                this.model = model;
                instance = new ModelInstance(model);

                if(model.animations.size >0){
                        animController = new AnimationController(instance);
                        desktopLauncher.setAnimList(model.animations);
                }else{
                        desktopLauncher.setAnimList(null);
                }

                //resetCam();
                if(label != null)
                        label.setText("");
        }

        private void onFontLoaded(BitmapFont font){
                stage = new Stage();
                table = new Table();
                table.setFillParent(true);
                stage.addActor(table);

                label = new Label("", new Label.LabelStyle(font, Color.BLACK));
                table.add(label);
        }

        public void resetCam(){
                cam = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
                cam.position.set(10f, 10f, 10f);
                cam.lookAt(0,0,0);
                cam.near = .1f;
                cam.far = 300f;
                cam.update();

                camController.camera = cam;
        }


        @Override
        public void resize(int width, int height) {
                super.resize(width, height);
                resetCam();
                if(stage != null)
                        stage.getViewport().update(width, height, true);
        }

        @Override
        public void dispose () {
                if(modelBatch != null)
                        modelBatch.dispose();
                if(model != null)
                        model.dispose();
                if(stage != null)
                        stage.dispose();
                if(assetManager != null)
                        assetManager.dispose();
        }



        public interface DesktopAppResolver {
                public void setAnimList(Array<Animation> animations);
        }
}
