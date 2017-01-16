package asf.modelpreview.desktop;

import com.badlogic.gdx.utils.Array;

import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Created by daniel on 1/15/17.
 */
class LogSidebar implements Log {

	private final DesktopLauncher desktopLauncher;

	private JTextPane outputTextPane;
	private JScrollPane outputTextScrollPane;

	private Array<Runnable> logBuffer;

	LogSidebar(DesktopLauncher desktopLauncher) {
		this.desktopLauncher = desktopLauncher;
		logBuffer = new Array<Runnable>(true, 8, Runnable.class);
	}

	JScrollPane buildUi(){
		outputTextPane = new JTextPane();
		outputTextPane.setEditable(false);
		outputTextScrollPane = new JScrollPane(outputTextPane);
		flushLogBuffer();
		return outputTextScrollPane;
	}

	@Override
	public void debug(Object obj){
		System.out.println(String.valueOf(obj));
	}

	@Override
	public void debugError(Throwable t, Object obj){
		System.out.println(String.valueOf(obj));
		t.printStackTrace();
	}

	private void flushLogBuffer(){
		Array<Runnable> logItemsTemp = logBuffer;
		logBuffer = null;
		for (int i = 0; i < logItemsTemp.size; i++) {
			SwingUtilities.invokeLater(logItemsTemp.get(i));
		}
	}

	@Override
	public void clear() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				if (logBuffer == null) {
					outputTextPane.setText("");
				} else {
					logBuffer.add(this);
				}
			}
		});
	}

	@Override
	public void clear(final String text) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				if(logBuffer == null) {
					outputTextPane.setText(text);
				}else{
					logBuffer.add(this);
				}
			}
		});
	}

	@Override
	public void text(final String text) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				if(logBuffer == null) {
					outputTextPane.setText(outputTextPane.getText() + "\n" + text);
				}else{
					logBuffer.add(this);
				}
			}
		});
	}

	@Override
	public void error(Throwable e) {

		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		error(sw.toString());
	}

	@Override
	public void error(Throwable e, String hintMessage) {

		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		error(sw.toString() + "\n" + hintMessage);
	}

	@Override
	public void error(final String text) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				if(logBuffer == null) {
					outputTextPane.setText(outputTextPane.getText() + "\n" + text);
					focus();
				}else{
					logBuffer.add(this);
				}
			}
		});
	}


	void focus(){
		desktopLauncher.requestFocus(outputTextScrollPane);
	}


}
