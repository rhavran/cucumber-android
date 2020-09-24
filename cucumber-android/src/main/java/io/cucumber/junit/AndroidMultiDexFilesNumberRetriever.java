package io.cucumber.junit;

import android.content.Context;
import android.content.SharedPreferences;

class AndroidMultiDexFilesNumberRetriever {
    private static final String PREFS_KEY_DEX_NUMBER = "dex.number";
    private static final String PREFS_SECTION_MULTI_DEX_VERSION_FILE = "multidex.version";

    private final SharedPreferences sharedPreferences;

    AndroidMultiDexFilesNumberRetriever(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_SECTION_MULTI_DEX_VERSION_FILE, Context.MODE_PRIVATE | Context.MODE_MULTI_PROCESS);
    }

    /**
     * @return number of multi dex files (N>=1) for current package.
     */
    int getNumberOfMultiDexFiles() {
        return sharedPreferences.getInt(PREFS_KEY_DEX_NUMBER, 1);
    }
}
