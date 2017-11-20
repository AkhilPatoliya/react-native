package com.facebook.react.bridge;

import com.facebook.react.bridge.JSBundleLoader;

import org.liquidplayer.javascript.JSFunction;

public class LiquidCoreBundleLoader extends JSBundleLoader {
    /**
     * Loads the script, returning the URL of the source it loaded.
     */
    public static LiquidCoreBundleLoader createLiquidCoreLoader(String srcUri,
                                                                Runnable startFunction) {
        return new LiquidCoreBundleLoader(srcUri, startFunction);
    }

    private final Runnable mStartFunction;
    private final String mSrcUri;

    private LiquidCoreBundleLoader(String srcUri, Runnable startFunction) {
        this.mStartFunction = startFunction;
        this.mSrcUri = srcUri;
    }

    @Override
    public String loadScript(CatalystInstanceImpl instance) {
        instance.setSourceURL(mSrcUri);
        if (mStartFunction != null) {
            mStartFunction.run();
        }
        return mSrcUri;
    }

}
