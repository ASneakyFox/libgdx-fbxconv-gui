package asf.modelpreview.desktop;

import javax.swing.filechooser.FileFilter;
import java.io.File;

/**
 * Created by Danny on 10/29/2014.
 */
public class BasicFileFilter extends FileFilter {
        private final String[] allowedExtensions;
        private final String description;

        public BasicFileFilter(String description, String... allowedExtensions) {
                this.description = description;
                this.allowedExtensions = allowedExtensions;
        }

        @Override
        public boolean accept(File f) {
                if(f.isDirectory())
                        return true;

                String fileName = f.getName().toLowerCase();

                for(String extension : allowedExtensions){
                        if(fileName.endsWith(extension)){
                                return true;
                        }
                }
                return false;
        }

        @Override
        public String getDescription() {
                return description;
        }
}
