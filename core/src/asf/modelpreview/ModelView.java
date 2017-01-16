package asf.modelpreview;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.FloatAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute;
import com.badlogic.gdx.graphics.g3d.model.Animation;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;

import java.io.File;

/**
 * Created by daniel on 1/16/17.
 */
public class ModelView implements View{
	private ModelWorld world;
	private AssetManager assetManager;

	private File file;
	private Model model;
	private ModelInstance modelInstance;
	private AnimationController animController;
	private boolean backFaceCulling = true;
	private boolean alphaBlending = false;
	private float alphaTest = -1;

	@Override
	public void create(ModelWorld world) {
		this.world = world;
		this.assetManager = world.assetManager;
	}

	@Override
	public void resize(int width, int height) {

	}

	@Override
	public void update(float delta) {
		if (modelInstance != null) {
			if (animController != null) {
				animController.update(delta);
			}
		}
	}

	@Override
	public void render(float delta) {
		if(modelInstance != null){
			world.modelBatch.render(modelInstance, world.environmentLightingEnabled ? world.environment : null);

		}

	}

	@Override
	public void dispose() {
		if (model != null)
			model.dispose();
	}

	public void setModel(File f, ModelWorld.LoadableListener onLoadedCallable){
		if (model != null) {
			if(this.file == null){
				model.dispose();
			}

			if(f!= null){
				try{
					world.absoluteAssetManager.unload(f.getAbsolutePath());
				}catch(Exception ex){
					//world.app.log.debugError(ex, "asset would probably not loaded before");
				}
			}
			model = null;
			modelInstance = null;
			animController = null;
		}

		this.file = f;

		if (f == null) {
			ModelBuilder modelBuilder = new ModelBuilder();
			model = modelBuilder.createBox(5f, 5f, 5f, new Material(ColorAttribute.createDiffuse(Color.GREEN)), VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
		} else {
			world.absoluteAssetManager.load(file.getAbsolutePath(), Model.class);

		}

		world.addAbsoluteLoadable(new ModelLoadable(onLoadedCallable));

	}

	private class ModelLoadable implements ModelWorld.Loadable {
		final ModelWorld.LoadableListener onLoadedCallable;

		public ModelLoadable(ModelWorld.LoadableListener onLoadedCallable) {
			this.onLoadedCallable = onLoadedCallable;
		}

		@Override
		public boolean onLoaded() {
			if(model == null && !world.absoluteAssetManager.isLoaded(file.getAbsolutePath(), Model.class)) {
				return false;
			}

			if(model == null){
				model = world.absoluteAssetManager.get(file.getAbsolutePath(), Model.class);
			}

			world.app.log.debug("loaded: "+file);

			modelInstance = new ModelInstance(model);

			Vector3 dimensions = modelInstance.calculateBoundingBox(new BoundingBox()).getDimensions(new Vector3());
			float largest = dimensions.x;
			if (dimensions.y > largest) largest = dimensions.y;
			if (dimensions.z > largest) largest = dimensions.z;
			if (largest > 25) {
				world.app.log.text("model is incredibly large, i suggest scaling it down");
			} else if (largest < 0.1f) {
				world.app.log.text("model is incredibly tiny, I suggest scaling it up.");
			}
				/*
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
                */

			setBackFaceCulling(backFaceCulling);
			setAlphaBlending(alphaBlending);
			setAlphaTest(alphaTest);

			if (model.animations.size > 0) {
				animController = new AnimationController(modelInstance);
				world.app.desktopLauncher.setAnimList(model.animations);
			} else {
				world.app.desktopLauncher.setAnimList(null);
			}

			//resetCam();
			world.hudView.showLoadingText("");

			if(onLoadedCallable != null) {
				onLoadedCallable.onLoaded();
			}

			return true;
		}
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

	public void setAlphaBlending(boolean alphaBlendingEnabled) {
		alphaBlending = alphaBlendingEnabled;
		if (modelInstance != null) {
			if (alphaBlending) {
				for (Material mat : modelInstance.materials) {
					mat.set(new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA));
				}
			} else {
				for (Material mat : modelInstance.materials) {
					mat.remove(BlendingAttribute.Type);
				}
			}
		}
	}

	public void setAlphaTest(float alphaTestValue) {
		alphaTest = alphaTestValue;
		if (modelInstance != null) {
			if (alphaTest >= 0) {
				for (Material mat : modelInstance.materials) {
					mat.set(new FloatAttribute(FloatAttribute.AlphaTest, alphaTest));
				}
			} else {
				for (Material mat : modelInstance.materials) {
					mat.remove(FloatAttribute.AlphaTest);
				}
			}
		}
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
}
