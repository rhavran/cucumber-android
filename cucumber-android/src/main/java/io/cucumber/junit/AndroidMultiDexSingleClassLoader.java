package io.cucumber.junit;

import android.content.Context;

import org.jetbrains.annotations.NotNull;

import dalvik.system.DexClassLoader;

class AndroidMultiDexSingleClassLoader {
    private static final String OPT_DEX_DIR = "optimizedDex";

    /**
     * The {@link DexClassLoader} to load classes from
     */
    private final ClassLoader classLoader;

    AndroidMultiDexSingleClassLoader(Context context, ClassLoader classLoader) {
        this.classLoader = new DexClassLoader(context.getPackageCodePath(),
                context.getDir(OPT_DEX_DIR, 0).getAbsolutePath(),
                null,
                classLoader);
    }

    <T> Class<? extends T> loadClass(@NotNull String className) throws ClassNotFoundException {
        return (Class<? extends T>) classLoader.loadClass(className);
    }
}
