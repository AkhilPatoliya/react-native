package com.facebook.react.bridge.queue;

/**
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

import android.os.Looper;

import com.facebook.common.logging.FLog;
import com.facebook.proguard.annotations.DoNotStrip;
import com.facebook.react.bridge.AssertionException;
import com.facebook.react.bridge.SoftAssertions;
import com.facebook.react.common.ReactConstants;
import com.facebook.react.common.futures.SimpleSettableFuture;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.liquidplayer.javascript.JSContext;

/**
 * Encapsulates a Thread that has a {@link Looper} running on it that can accept Runnables.
 */
@DoNotStrip
public class LiquidCoreMessageQueueThreadImpl implements MessageQueueThread {

    private final String mName;
    private final String mAssertionErrorMessage;
    private volatile boolean mIsFinished = false;
    private final JSContext mJsContext;

    private LiquidCoreMessageQueueThreadImpl(
            String name,
            JSContext jsContext,
            QueueThreadExceptionHandler exceptionHandler) {
        mJsContext = jsContext;
        mName = name;
        mAssertionErrorMessage = "Expected to be called from the '" + getName() + "' thread!";
    }

    /**
     * Runs the given Runnable on this Thread. It will be submitted to the end of the event queue even
     * if it is being submitted from the same queue Thread.
     */
    @DoNotStrip
    @Override
    public void runOnQueue(Runnable runnable) {
        if (mIsFinished) {
            FLog.w(
                    ReactConstants.TAG,
                    "Tried to enqueue runnable on already finished thread: '" + getName() +
                            "... dropping Runnable.");
        }
        mJsContext.async(runnable);
    }

    @DoNotStrip
    @Override
    public <T> Future<T> callOnQueue(final Callable<T> callable) {
        final SimpleSettableFuture<T> future = new SimpleSettableFuture<>();
        runOnQueue(
                new Runnable() {
                    @Override
                    public void run() {
                        try {
                            future.set(callable.call());
                        } catch (Exception e) {
                            future.setException(e);
                        }
                    }
                });
        return future;
    }

    /**
     * @return whether the current Thread is also the Thread associated with this MessageQueueThread.
     */
    @DoNotStrip
    @Override
    public boolean isOnThread() {
        return mJsContext.isOnThread();
    }

    /**
     * Asserts {@link #isOnThread()}, throwing a {@link AssertionException} (NOT an
     * {@link AssertionError}) if the assertion fails.
     */
    @DoNotStrip
    @Override
    public void assertIsOnThread() {
        SoftAssertions.assertCondition(isOnThread(), mAssertionErrorMessage);
    }

    /**
     * Asserts {@link #isOnThread()}, throwing a {@link AssertionException} (NOT an
     * {@link AssertionError}) if the assertion fails.
     */
    @DoNotStrip
    @Override
    public void assertIsOnThread(String message) {
        SoftAssertions.assertCondition(
                isOnThread(),
                new StringBuilder().append(mAssertionErrorMessage).append(" ").append(message).toString());
    }

    /**
     * Quits this queue's Looper. If that Looper was running on a different Thread than the current
     * Thread, also waits for the last message being processed to finish and the Thread to die.
     */
    @DoNotStrip
    @Override
    public void quitSynchronous() {
        mIsFinished = true;
        // FIXME: Not sure we should do this
        /*
        mLooper.quit();
        if (mLooper.getThread() != Thread.currentThread()) {
            try {
                mLooper.getThread().join();
            } catch (InterruptedException e) {
                throw new RuntimeException("Got interrupted waiting to join thread " + mName);
            }
        }
        */
    }

    public String getName() {
        return mName;
    }

    public static LiquidCoreMessageQueueThreadImpl create(
            JSContext jsContext,
            MessageQueueThreadSpec spec,
            QueueThreadExceptionHandler exceptionHandler) {

        return new LiquidCoreMessageQueueThreadImpl(spec.getName(), jsContext, exceptionHandler);
    }
}
