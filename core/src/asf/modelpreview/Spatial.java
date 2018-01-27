package asf.modelpreview;

import com.badlogic.gdx.Gdx;
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
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.Disposable;

import java.io.File;

public class Spatial implements Disposable {

	public Model model;
	public ModelInstance modelInstance;
	public AnimationController animController;
	public boolean backFaceCulling = true;
	public boolean alphaBlending = false;
	public float alphaTest = -1;

	public Vector3 dimensions;
	public final Vector3 translation = new Vector3();
	public final Quaternion rotation = new Quaternion();
	public float scalingFactor = 1;

	public void init() {
		modelInstance = new ModelInstance(model);

		dimensions = modelInstance.calculateBoundingBox(new BoundingBox()).getDimensions(new Vector3());
		float largest = dimensions.x;
		if (dimensions.y > largest) largest = dimensions.y;
		if (dimensions.z > largest) largest = dimensions.z;
		if (largest > 25) {
			float s = 25f / largest;
			scalingFactor = s;
			modelInstance.transform.setToScaling(s, s, s);
		} else if (largest < 0.1f) {
			float s = 5 / largest;
			scalingFactor = s;
			modelInstance.transform.setToScaling(s, s, s);
		}

		setBackFaceCulling(backFaceCulling);
		setAlphaBlending(alphaBlending);
		setAlphaTest(alphaTest);

		if(model.animations.size > 0) {
			animController = new AnimationController(modelInstance);
		}

	}



	public void setLocation(Vector3 loc){
		this.translation.set(loc);

		modelInstance.transform.set(
			translation.x, translation.y, translation.z,
			rotation.x, rotation.y, rotation.z, rotation.w,
			scalingFactor, scalingFactor, scalingFactor
		);
	}

	public void render(float delta, ModelPreviewApp world){
		if(modelInstance != null) {
			if(animController != null) {
				animController.update(delta);
			}
			world.modelBatch.render(modelInstance, world.environmentLightingEnabled ? world.environment : null);
		}
	}

	public void setBackFaceCulling(boolean backFaceCullingEnabled) {
		this.backFaceCulling = backFaceCullingEnabled;
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
		this.alphaBlending = alphaBlendingEnabled;
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

	@Override
	public void dispose() {
		if (model != null) {
			model.dispose();
			model = null;
			animController = null;
		}
	}
}
