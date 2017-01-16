package asf.modelpreview.desktop;

/**
 * Created by daniel on 1/15/17.
 */
public interface Log {
	void debug(Object obj);

	void debugError(Throwable t, Object obj);

	void clear();

	void clear(final String text);

	void text(final String text);

	void error(Throwable e);

	void error(Throwable e, String hintMessage);

	void error(final String text);
}
