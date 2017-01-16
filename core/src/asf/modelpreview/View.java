package asf.modelpreview;

import com.badlogic.gdx.utils.Disposable;

/**
 * Created by daniel on 1/16/17.
 */
public interface View extends Disposable{

	void create(ModelWorld world);

	void resize(int width, int height);

	void update(float delta);

	void render(float delta);

}
