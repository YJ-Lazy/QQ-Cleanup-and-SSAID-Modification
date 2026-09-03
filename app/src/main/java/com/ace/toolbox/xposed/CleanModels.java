package com.ace.toolbox.xposed;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

final class CleanModels {
    static final class Root {
        final String label;
        final File file;
        Root(String label, File file) {
            this.label = label;
            this.file = file;
        }
    }

    /**
     * One selectable cleanup item shown inside QQ.
     * A target may map to multiple equivalent paths across app-specific and legacy storage.
     */
    static final class Target {
        final String id;
        final String category;
        final String label;
        final String description;
        final boolean defaultSelected;
        final boolean deepClean;
        final List<File> roots = new ArrayList<>();

        Target(
                String id,
                String category,
                String label,
                String description,
                boolean defaultSelected,
                boolean deepClean
        ) {
            this.id = id;
            this.category = category;
            this.label = label;
            this.description = description;
            this.defaultSelected = defaultSelected;
            this.deepClean = deepClean;
        }
    }

    static final class TargetScan {
        final Target target;
        long bytes;
        int files;

        TargetScan(Target target) {
            this.target = target;
        }
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
        int failedFiles;
        long freedBytes;
    }

    private CleanModels() {}
}
