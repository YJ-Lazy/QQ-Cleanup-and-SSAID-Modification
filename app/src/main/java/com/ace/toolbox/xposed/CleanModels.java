package com.ace.toolbox.xposed;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

final class CleanModels {
    static final class Root {
        final String label;
        final File file;
        Root(String label, File file) { this.label = label; this.file = file; }
    }

    static final class Scan {
        long bytes;
        int files;
        final List<String> accounts = new ArrayList<>();
    }

    interface Progress {
        void onProgress(int deletedFiles, int totalFiles, long freedBytes);
    }

    static final class Result {
        int deletedFiles;
        long freedBytes;
    }

    private CleanModels() {}
}
