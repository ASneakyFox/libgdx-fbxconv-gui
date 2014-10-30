package asf.modelpreview;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.loader.G3dModelLoader;
import com.badlogic.gdx.graphics.g3d.loader.ObjLoader;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.UBJsonReader;

import java.io.File;

public class ModelPreviewApp extends ApplicationAdapter {
        public PerspectiveCamera cam;
        public Environment environment;
        public volatile boolean environmentLightingEnabled = true;
        public final Color backgroundColor = new Color(0,0,0,1);
        public ModelBatch modelBatch;
        public Model model;
        public ModelInstance instance;
        CameraInputController camController;

        private G3dModelLoader g3dbModelLoader;
        private G3dModelLoader g3djModelLoader;
        private ObjLoader objLoader;


        public void previewFile(File f) throws GdxRuntimeException {
                if(model != null){
                        model.dispose();
                        model = null;
                        instance = null;
                }

                if(f == null){
                        ModelBuilder modelBuilder = new ModelBuilder();
                        model = modelBuilder.createBox(5f, 5f, 5f,
                                new Material(ColorAttribute.createDiffuse(Color.GREEN)),
                                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
                        instance = new ModelInstance(model);
                }else{
                        String absolutePath = f.getAbsolutePath();
                        //System.out.println("abs path: "+absolutePath);
                        if(absolutePath.toLowerCase().endsWith("obj")){
                                model = objLoader.loadModel(Gdx.files.absolute(absolutePath));
                        }else if(absolutePath.toLowerCase().endsWith("g3dj")){
                                model = g3djModelLoader.loadModel(Gdx.files.absolute(absolutePath));
                        }else{
                                model = g3dbModelLoader.loadModel(Gdx.files.absolute(absolutePath));
                        }
                        instance = new ModelInstance(model);
                }

                resetCam();

        }


	@Override
	public void create () {
                objLoader = new ObjLoader();
                g3dbModelLoader = new G3dModelLoader(new UBJsonReader());
                g3djModelLoader = new G3dModelLoader(new JsonReader());

                modelBatch = new ModelBatch();

                camController = new CameraInputController(null);
                Gdx.input.setInputProcessor(camController);

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
                camController.update();

                Gdx.gl.glClearColor(backgroundColor.r,backgroundColor.g,backgroundColor.b,backgroundColor.a);
                Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
                Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);


                if(instance != null){
                        modelBatch.begin(cam);
                        modelBatch.render(instance, environmentLightingEnabled ? environment : null);
                        modelBatch.end();
                }
	}

        private void resetCam(){
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
        }

        @Override
        public void dispose () {
                modelBatch.dispose();
                if(model != null)
                        model.dispose();
        }
}
