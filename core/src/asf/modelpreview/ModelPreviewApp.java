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
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.FloatAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.loader.G3dModelLoader;
import com.badlogic.gdx.graphics.g3d.loader.ObjLoader;
import com.badlogic.gdx.graphics.g3d.model.Animation;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.UBJsonReader;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import java.io.File;

public class ModelPreviewApp extends ApplicationAdapter {
        private final DesktopAppResolver desktopLauncher;
        private PerspectiveCamera cam;
        private CameraInputController camController;

        public Environment environment;
        public volatile boolean environmentLightingEnabled = true;
        public final Color backgroundColor = new Color(100 / 255f, 149 / 255f, 237 / 255f, 1f);

        private ModelBatch modelBatch;

        private Model model;
        private ModelInstance modelInstance;
        private AnimationController animController;
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

                if (modelInstance != null) {
                        if (backFaceCulling) {
                                for (Material mat : modelInstance.materials) {
                                        mat.remove(IntAttribute.CullFace);
                                }
                        } else {
                                for (Material mat : modelInstance.materials) {
                                        mat.set(new IntAttribute(IntAttribute.CullFace, 0));
                                }
                        }
                }
        }

        public void setAlphaBlending(boolean alphaBlendingEnabled){
                alphaBlending = alphaBlendingEnabled;
                if(modelInstance != null){
                        if(alphaBlending){
                                for (Material mat : modelInstance.materials) {
                                        mat.set(new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA));
                                }
                        }else{
                                for (Material mat : modelInstance.materials) {
                                        mat.remove(BlendingAttribute.Type);
                                }
                        }
                }
        }

        public void setAlphaTest(float alphaTestValue){
                alphaTest = alphaTestValue;
                if(modelInstance != null){
                        if(alphaTest>=0){
                                for (Material mat : modelInstance.materials) {
                                        mat.set(new FloatAttribute(FloatAttribute.AlphaTest, alphaTest));
                                }
                        }else{
                                for (Material mat : modelInstance.materials) {
                                        mat.remove(FloatAttribute.AlphaTest);
                                }
                        }
                }
        }



        public void setBackgroundColor(float r, float g, float b){
                backgroundColor.set(r,g,b,1);
                Gdx.gl.glClearColor(backgroundColor.r,backgroundColor.g,backgroundColor.b,backgroundColor.a);
        }

        public void resetBackgroundColor(){
                setBackgroundColor(100 / 255f, 149 / 255f, 237 / 255f);
        }

        public void previewFile(File f) throws GdxRuntimeException {
                if (model != null) {
                        model.dispose();
                        model = null;
                        animController = null;
                }

                if (f == null) {
                        ModelBuilder modelBuilder = new ModelBuilder();
                        model = modelBuilder.createBox(5f, 5f, 5f,
                                new Material(ColorAttribute.createDiffuse(Color.GREEN)),
                                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
                } else {
                        String absolutePath = f.getAbsolutePath();

                        if (absolutePath.toLowerCase().endsWith("obj")) {
                                model = objLoader.loadModel(Gdx.files.absolute(absolutePath));
                        } else if (absolutePath.toLowerCase().endsWith("g3dj")) {
                                model = g3djModelLoader.loadModel(Gdx.files.absolute(absolutePath));
                        } else {
                                model = g3dbModelLoader.loadModel(Gdx.files.absolute(absolutePath));
                        }


                }


                onModelLoaded(model);


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


                try {
                        previewFile(null);
                } catch (GdxRuntimeException e) {
                        e.printStackTrace();
                }


                environment = new Environment();
                environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
                environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));

                Gdx.gl.glClearColor(backgroundColor.r, backgroundColor.g, backgroundColor.b, backgroundColor.a);

        }

        @Override
        public void render() {
                if (assetManager.update()) {
                        if (stage == null && assetManager.isLoaded("loadingFont.ttf")) {
                                onFontLoaded(assetManager.get("loadingFont.ttf", BitmapFont.class));
                        }
                }

                camController.update();

                Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

                if (modelInstance != null) {
                        if (animController != null) {
                                animController.update(Gdx.graphics.getDeltaTime());
                        }
                        modelBatch.begin(cam);
                        modelBatch.render(modelInstance, environmentLightingEnabled ? environment : null);
                        modelBatch.end();
                }

                if (stage != null)
                        stage.draw();


        }

        public void setAnimation(Animation selectedItem) {
                if (animController == null)
                        return;

                if (selectedItem == null) {
                        animController.setAnimation(null, -1);
                } else {
                        animController.setAnimation(selectedItem.id, -1);
                }


        }

        private void onModelLoaded(Model model) {
                this.model = model;
                modelInstance = new ModelInstance(model);

                Vector3 dimensions = modelInstance.calculateBoundingBox(new BoundingBox()).getDimensions(new Vector3());
                float largest = dimensions.x;
                if(dimensions.y > largest) largest = dimensions.y;
                if(dimensions.z > largest) largest= dimensions.z;
                if(largest > 25){
                        float s = 25f / largest;
                        modelInstance.transform.setToScaling(s, s, s);
                        if(infoLabel != null)
                                infoLabel.setText("Scaled to: "+(s * 100f)+"%");
                }else if(largest < 0.1f) {
                        float s = 5 / largest;
                        modelInstance.transform.setToScaling(s, s, s);
                        if(infoLabel != null)
                                infoLabel.setText("Scaled to: "+(s * 100f)+"%");
                }else{
                        if(infoLabel != null)
                                infoLabel.setText("");
                }

                setBackFaceCulling(backFaceCulling);
                setAlphaBlending(alphaBlending);
                setAlphaTest(alphaTest);

                if (model.animations.size > 0) {
                        animController = new AnimationController(modelInstance);
                        desktopLauncher.setAnimList(model.animations);
                } else {
                        desktopLauncher.setAnimList(null);
                }

                //resetCam();
                if (label != null)
                        label.setText("");
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
                c2.pad(0,10,2,0);
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
                if (model != null)
                        model.dispose();
                if (stage != null)
                        stage.dispose();
                if (assetManager != null)
                        assetManager.dispose();
        }


        public interface DesktopAppResolver {
                public void setAnimList(Array<Animation> animations);
        }
}
