package asf.modelpreview;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g3d.model.Animation;
import com.badlogic.gdx.utils.Array;

public class ModelPreviewApp implements ApplicationListener {
	final DesktopAppResolver desktopLauncher;
	final Log log;
	public ModelWorld world;



	public ModelPreviewApp(DesktopAppResolver desktopLauncher) {
		this.desktopLauncher = desktopLauncher;
		this.log = desktopLauncher.getLog();
		world = new ModelWorld(this);
	}

	@Override
	public void create() {
		world.create();
	}

	@Override
	public void resize(int width, int height) {
		world.resize(width, height);
	}


	@Override
	public void render() {
		float delta = Gdx.graphics.getDeltaTime();
		if(delta > 0.06f) delta = 0.06f;
		world.render(delta);
	}

	@Override
	public void pause() {

	}

	@Override
	public void resume() {

	}

	@Override
	public void dispose() {
		if(world != null) {
			world.dispose();
			world = null;
		}
	}

	public interface DesktopAppResolver {
		public Log getLog();

		public void setAnimList(Array<Animation> animations);
	}
}
