package com.acme.claims.util;

import java.io.File;
import java.io.IOException;

/**
 * Wraps a temporary file and deletes it when the handle is garbage collected.
 */
public class TempFileHandle {

    private final File file;

    public TempFileHandle(String prefix, String suffix) throws IOException {
        this.file = File.createTempFile(prefix, suffix);
    }

    public File getFile() {
        return file;
    }

    public String getPath() {
        return file.getAbsolutePath();
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            if (file.exists()) {
                file.delete();
            }
        } finally {
            super.finalize();
        }
    }
}
