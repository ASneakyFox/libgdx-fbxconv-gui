package asf.modelpreview.desktop;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

public class DisplayLabelManager {
	ResourceBundle i18n;

	public void loadLabels(String keyI18n, String locale) {
		i18n =  ResourceBundle.getBundle(keyI18n, new Locale(locale));
	}

	public String get(String key){
		return i18n.getString(key);
	}

	public String get(String key, Object... params){
		return MessageFormat.format(key, params);
	}
}
