package io.cucumber.junit;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import cucumber.runtime.CucumberException;
import dalvik.system.DexFile;

/**
 * {@see https://android.googlesource.com/platform/frameworks/multidex/+/8c2abf7/library/src/android/support/multidex/MultiDexExtractor.java}
 */
class AndroidMultiDexAllClassesLoader {
    private static final String FILE_EXT_CLASSES = ".classes";
    private static final String FILE_EXT_TEMP = ".tmp";
    private static final String FILE_EXT_ZIP = ".zip";
    private static final String SECONDARY_DEX_FOLDER_NAME = "code_cache" + File.separator + "secondary-dexes";

    private final Context context;
    private final AndroidMultiDexFilesNumberRetriever multiDexSizeRetriever;

    public AndroidMultiDexAllClassesLoader(Context context, AndroidMultiDexFilesNumberRetriever multiDexSizeRetriever) {
        this.context = context;
        this.multiDexSizeRetriever = multiDexSizeRetriever;
    }

    /**
     * get all the classes name in "classes.dex", "classes2.dex", ....
     *
     * @return all the classes name
     */
    List<String> loadAllClasses() throws PackageManager.NameNotFoundException {
        List<String> classNames = new ArrayList<>();
        for (String path : getSourcePaths()) {
            try {
                DexFile dexfile;
                if (path.endsWith(FILE_EXT_ZIP)) {
                    //Do not use new DexFile(path), because it will throw "permission error in /data/dalvik-cache"
                    dexfile = DexFile.loadDex(path, path + FILE_EXT_TEMP, 0);
                } else {
                    dexfile = new DexFile(path);
                }
                Enumeration<String> dexEntries = dexfile.entries();
                if (dexEntries == null) {
                    throw new CucumberException("Error at loading dex file '" + path + "'");
                }
                while (dexEntries.hasMoreElements()) {
                    classNames.add(dexEntries.nextElement());
                }
            } catch (IOException e) {
                throw new CucumberException("Error at loading dex file '" + path + "'");
            }
        }
        return classNames;
    }

    /**
     * Get all the dex path
     *
     * @return all the dex path
     */
    @NotNull
    private List<String> getSourcePaths() throws PackageManager.NameNotFoundException {
        List<String> sourcePaths = new ArrayList<>();

        ApplicationInfo applicationInfo = context.getPackageManager().getApplicationInfo(context.getPackageName(), 0);
        // Add the default apk path.
        sourcePaths.add(applicationInfo.sourceDir);

        File sourceApk = new File(applicationInfo.sourceDir);
        // The prefix of extracted file, ie: test.classes
        String extractedFilePrefix = sourceApk.getName() + FILE_EXT_CLASSES;

        // Get the total dex files number.
        int totalNumberOfDexFiles = multiDexSizeRetriever.getNumberOfMultiDexFiles();
        File dexDir = new File(applicationInfo.dataDir, SECONDARY_DEX_FOLDER_NAME);
        for (int secondaryDexFileIndex = 2; secondaryDexFileIndex <= totalNumberOfDexFiles; secondaryDexFileIndex++) {
            // For each dex file, ie: test.classes2.zip, test.classes3.zip...
            File dexFile = new File(dexDir, extractedFilePrefix + secondaryDexFileIndex + FILE_EXT_ZIP);
            if (dexFile.isFile()) {
                sourcePaths.add(dexFile.getAbsolutePath());
                //we ignore the verify zip part
            } else {
                throw new CucumberException("Missing extracted secondary dex file '" + dexFile.getPath() + "'");
            }
        }

        return sourcePaths;
    }
}
