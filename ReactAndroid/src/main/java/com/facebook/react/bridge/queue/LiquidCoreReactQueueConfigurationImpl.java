/**
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.react.bridge.queue;

import javax.annotation.Nullable;

import java.util.Map;

import android.os.Looper;

import com.facebook.react.common.MapBuilder;

import org.liquidplayer.javascript.JSContext;

public class LiquidCoreReactQueueConfigurationImpl implements ReactQueueConfiguration {

    private final MessageQueueThreadImpl mUIQueueThread;
    private final @Nullable MessageQueueThreadImpl mUIBackgroundQueueThread;
    private final MessageQueueThreadImpl mNativeModulesQueueThread;
    private final LiquidCoreMessageQueueThreadImpl mJSQueueThread;

    private LiquidCoreReactQueueConfigurationImpl(
            MessageQueueThreadImpl uiQueueThread,
            @Nullable MessageQueueThreadImpl uiBackgroundQueueThread,
            MessageQueueThreadImpl nativeModulesQueueThread,
            LiquidCoreMessageQueueThreadImpl jsQueueThread) {
        mUIQueueThread = uiQueueThread;
        mUIBackgroundQueueThread = uiBackgroundQueueThread;
        mNativeModulesQueueThread = nativeModulesQueueThread;
        mJSQueueThread = jsQueueThread;
    }

    @Override
    public MessageQueueThread getUIQueueThread() {
        return mUIQueueThread;
    }

    @Override
    public @Nullable MessageQueueThread getUIBackgroundQueueThread() {
        return mUIBackgroundQueueThread;
    }

    @Override
    public MessageQueueThread getNativeModulesQueueThread() {
        return mNativeModulesQueueThread;
    }

    @Override
    public MessageQueueThread getJSQueueThread() {
        return mJSQueueThread;
    }

    /**
     * Should be called when the corresponding {@link com.facebook.react.bridge.CatalystInstance}
     * is destroyed so that we shut down the proper queue threads.
     */
    public void destroy() {
        if (mUIBackgroundQueueThread != null &&
                mUIBackgroundQueueThread.getLooper() != Looper.getMainLooper()) {
            mUIBackgroundQueueThread.quitSynchronous();
        }
        if (mNativeModulesQueueThread.getLooper() != Looper.getMainLooper()) {
            mNativeModulesQueueThread.quitSynchronous();
        }
        /* FIXME
        if (mJSQueueThread.getLooper() != Looper.getMainLooper()) {
            mJSQueueThread.quitSynchronous();
        }
        */
    }

    public static LiquidCoreReactQueueConfigurationImpl create(
            JSContext jsContext,
            ReactQueueConfigurationSpec spec,
            QueueThreadExceptionHandler exceptionHandler) {
        Map<MessageQueueThreadSpec, MessageQueueThreadImpl> specsToThreads = MapBuilder.newHashMap();

        MessageQueueThreadSpec uiThreadSpec = MessageQueueThreadSpec.mainThreadSpec();
        MessageQueueThreadImpl uiThread =
                MessageQueueThreadImpl.create(uiThreadSpec, exceptionHandler);
        specsToThreads.put(uiThreadSpec, uiThread);

        /*
        MessageQueueThreadImpl jsThread = specsToThreads.get(spec.getJSQueueThreadSpec());
        if (jsThread == null) {
            jsThread = MessageQueueThreadImpl.create(spec.getJSQueueThreadSpec(), exceptionHandler);
        }
        */
        LiquidCoreMessageQueueThreadImpl jsThread = LiquidCoreMessageQueueThreadImpl.create(
                jsContext, spec.getJSQueueThreadSpec(), exceptionHandler);

        MessageQueueThreadImpl nativeModulesThread =
                specsToThreads.get(spec.getNativeModulesQueueThreadSpec());
        if (nativeModulesThread == null) {
            nativeModulesThread =
                    MessageQueueThreadImpl.create(spec.getNativeModulesQueueThreadSpec(), exceptionHandler);
        }

        MessageQueueThreadImpl uiBackgroundThread =
                specsToThreads.get(spec.getUIBackgroundQueueThreadSpec());
        if (uiBackgroundThread == null && spec.getUIBackgroundQueueThreadSpec() != null) {
            uiBackgroundThread =
                    MessageQueueThreadImpl.create(spec.getUIBackgroundQueueThreadSpec(), exceptionHandler);
        }

        return new LiquidCoreReactQueueConfigurationImpl(
                uiThread,
                uiBackgroundThread,
                nativeModulesThread,
                jsThread);
    }
}
