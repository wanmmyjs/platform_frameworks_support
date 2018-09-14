/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.work;

import android.net.Network;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.annotation.RestrictTo;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;

/**
 * Setup parameters for a {@link Worker}.
 */
public final class WorkerParameters {

    private @NonNull UUID mId;
    private @NonNull Data mInputData;
    private @NonNull Set<String> mTags;
    private @NonNull RuntimeExtras mRuntimeExtras;
    private int mRunAttemptCount;
    private @NonNull Executor mBackgroundExecutor;

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public WorkerParameters(
            @NonNull UUID id,
            @NonNull Data inputData,
            @NonNull Collection<String> tags,
            @NonNull RuntimeExtras runtimeExtras,
            int runAttemptCount,
            @NonNull Executor backgroundExecutor) {
        mId = id;
        mInputData = inputData;
        mTags = new HashSet<>(tags);
        mRuntimeExtras = runtimeExtras;
        mRunAttemptCount = runAttemptCount;
        mBackgroundExecutor = backgroundExecutor;
    }

    /**
     * Gets the ID of the {@link WorkRequest} that created this Worker.
     *
     * @return The ID of the creating {@link WorkRequest}
     */
    public @NonNull UUID getId() {
        return mId;
    }

    /**
     * Gets the input data.  Note that in the case that there are multiple prerequisites for this
     * Worker, the input data has been run through an {@link InputMerger}.
     *
     * @return The input data for this work
     * @see OneTimeWorkRequest.Builder#setInputMerger(Class)
     */
    public @NonNull Data getInputData() {
        return mInputData;
    }

    /**
     * Gets a {@link java.util.Set} of tags associated with this Worker's {@link WorkRequest}.
     *
     * @return The {@link java.util.Set} of tags associated with this Worker's {@link WorkRequest}
     * @see WorkRequest.Builder#addTag(String)
     */
    public @NonNull Set<String> getTags() {
        return mTags;
    }

    /**
     * Gets the array of content {@link android.net.Uri}s that caused this Worker to execute
     *
     * @return The array of content {@link android.net.Uri}s that caused this Worker to execute
     * @see Constraints.Builder#addContentUriTrigger(android.net.Uri, boolean)
     */
    @RequiresApi(24)
    public @Nullable Uri[] getTriggeredContentUris() {
        return mRuntimeExtras.triggeredContentUris;
    }

    /**
     * Gets the array of content authorities that caused this Worker to execute
     *
     * @return The array of content authorities that caused this Worker to execute
     */
    @RequiresApi(24)
    public @Nullable String[] getTriggeredContentAuthorities() {
        return mRuntimeExtras.triggeredContentAuthorities;
    }

    /**
     * Gets the {@link android.net.Network} to use for this Worker.
     * This method returns {@code null} if there is no network needed for this work request.
     *
     * @return The {@link android.net.Network} specified by the OS to be used with this Worker
     */
    @RequiresApi(28)
    public @Nullable Network getNetwork() {
        return mRuntimeExtras.network;
    }

    /**
     * Gets the current run attempt count for this work.
     *
     * @return The current run attempt count for this work.
     */
    public int getRunAttemptCount() {
        return mRunAttemptCount;
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public @NonNull RuntimeExtras getRuntimeExtras() {
        return mRuntimeExtras;
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public @NonNull Executor getBackgroundExecutor() {
        return mBackgroundExecutor;
    }

    /**
     * Extra runtime information for Workers.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static class RuntimeExtras {
        public String[] triggeredContentAuthorities;
        public Uri[] triggeredContentUris;

        @RequiresApi(28)
        public Network network;
    }
}