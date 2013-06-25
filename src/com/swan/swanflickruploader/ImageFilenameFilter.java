package com.swan.swanflickruploader;

import java.io.File;
import java.io.FilenameFilter;

public class ImageFilenameFilter implements FilenameFilter {
    String[] extension = { "jpg", "jpeg", "gif", "png", "mov", "avi", "mp4"};

    @Override
    public boolean accept(File arg0, String arg1) {
        boolean fileOK = true;

        for (int i = 0; i < extension.length; i++) {
            if (extension != null) {
                fileOK = arg1.toLowerCase().endsWith('.' + extension[i]);

                if (fileOK)
                    break;
            }
        }
        return fileOK;

    }

}
