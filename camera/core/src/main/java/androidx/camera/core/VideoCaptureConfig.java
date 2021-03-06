/*
 * Copyright (C) 2019 The Android Open Source Project
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

package androidx.camera.core;

import android.os.Handler;
import android.util.Rational;
import android.util.Size;

import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;

import java.util.Set;
import java.util.UUID;

/** Configuration for a video capture use case.
 *
 * @hide In the earlier stage, the VideoCapture is deprioritized.
 */
@RestrictTo(Scope.LIBRARY_GROUP)
public final class VideoCaptureConfig
        implements UseCaseConfig<VideoCapture>,
        ImageOutputConfig,
        CameraDeviceConfig,
        ThreadConfig {

    // Option Declarations:
    // *********************************************************************************************
    static final Option<Integer> OPTION_VIDEO_FRAME_RATE =
            Option.create("camerax.core.videoCapture.recordingFrameRate", int.class);
    static final Option<Integer> OPTION_BIT_RATE =
            Option.create("camerax.core.videoCapture.bitRate", int.class);
    static final Option<Integer> OPTION_INTRA_FRAME_INTERVAL =
            Option.create("camerax.core.videoCapture.intraFrameInterval", int.class);
    static final Option<Integer> OPTION_AUDIO_BIT_RATE =
            Option.create("camerax.core.videoCapture.audioBitRate", int.class);
    static final Option<Integer> OPTION_AUDIO_SAMPLE_RATE =
            Option.create("camerax.core.videoCapture.audioSampleRate", int.class);
    static final Option<Integer> OPTION_AUDIO_CHANNEL_COUNT =
            Option.create("camerax.core.videoCapture.audioChannelCount", int.class);
    static final Option<Integer> OPTION_AUDIO_RECORD_SOURCE =
            Option.create("camerax.core.videoCapture.audioRecordSource", int.class);
    static final Option<Integer> OPTION_AUDIO_MIN_BUFFER_SIZE =
            Option.create("camerax.core.videoCapture.audioMinBufferSize", int.class);
    private final OptionsBundle mConfig;

    VideoCaptureConfig(OptionsBundle config) {
        mConfig = config;
    }

    /**
     * {@inheritDoc}
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    public Config getConfig() {
        return mConfig;
    }

    /**
     * Returns the recording frames per second.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or <code>valueIfMissing</code> if the value does not exist in this
     * configuration.
     */
    public int getVideoFrameRate(int valueIfMissing) {
        return getConfig().retrieveOption(OPTION_VIDEO_FRAME_RATE, valueIfMissing);
    }

    /**
     * Returns the recording frames per second.
     *
     * @return The stored value, if it exists in this configuration.
     * @throws IllegalArgumentException if the option does not exist in this configuration.
     */
    public int getVideoFrameRate() {
        return getConfig().retrieveOption(OPTION_VIDEO_FRAME_RATE);
    }

    /**
     * Returns the encoding bit rate.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or <code>valueIfMissing</code> if the value does not exist in this
     * configuration.
     */
    public int getBitRate(int valueIfMissing) {
        return getConfig().retrieveOption(OPTION_BIT_RATE, valueIfMissing);
    }

    /**
     * Returns the encoding bit rate.
     *
     * @return The stored value, if it exists in this configuration.
     * @throws IllegalArgumentException if the option does not exist in this configuration.
     */
    public int getBitRate() {
        return getConfig().retrieveOption(OPTION_BIT_RATE);
    }

    /**
     * Returns the number of seconds between each key frame.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or <code>valueIfMissing</code> if the value does not exist in this
     * configuration.
     */
    public int getIFrameInterval(int valueIfMissing) {
        return getConfig().retrieveOption(OPTION_INTRA_FRAME_INTERVAL, valueIfMissing);
    }

    /**
     * Returns the number of seconds between each key frame.
     *
     * @return The stored value, if it exists in this configuration.
     * @throws IllegalArgumentException if the option does not exist in this configuration.
     */
    public int getIFrameInterval() {
        return getConfig().retrieveOption(OPTION_INTRA_FRAME_INTERVAL);
    }

    /**
     * Returns the audio encoding bit rate.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or <code>valueIfMissing</code> if the value does not exist in this
     * configuration.
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public int getAudioBitRate(int valueIfMissing) {
        return getConfig().retrieveOption(OPTION_AUDIO_BIT_RATE, valueIfMissing);
    }

    /**
     * Returns the audio encoding bit rate.
     *
     * @return The stored value, if it exists in this configuration.
     * @throws IllegalArgumentException if the option does not exist in this configuration.
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public int getAudioBitRate() {
        return getConfig().retrieveOption(OPTION_AUDIO_BIT_RATE);
    }

    /**
     * Returns the audio sample rate.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or <code>valueIfMissing</code> if the value does not exist in this
     * configuration.
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public int getAudioSampleRate(int valueIfMissing) {
        return getConfig().retrieveOption(OPTION_AUDIO_SAMPLE_RATE, valueIfMissing);
    }

    /**
     * Returns the audio sample rate.
     *
     * @return The stored value, if it exists in this configuration.
     * @throws IllegalArgumentException if the option does not exist in this configuration.
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public int getAudioSampleRate() {
        return getConfig().retrieveOption(OPTION_AUDIO_SAMPLE_RATE);
    }

    /**
     * Returns the audio channel count.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or <code>valueIfMissing</code> if the value does not exist in this
     * configuration.
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public int getAudioChannelCount(int valueIfMissing) {
        return getConfig().retrieveOption(OPTION_AUDIO_CHANNEL_COUNT, valueIfMissing);
    }

    /**
     * Returns the audio channel count.
     *
     * @return The stored value, if it exists in this configuration.
     * @throws IllegalArgumentException if the option does not exist in this configuration.
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public int getAudioChannelCount() {
        return getConfig().retrieveOption(OPTION_AUDIO_CHANNEL_COUNT);
    }

    /**
     * Returns the audio recording source.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or <code>valueIfMissing</code> if the value does not exist in this
     * configuration.
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public int getAudioRecordSource(int valueIfMissing) {
        return getConfig().retrieveOption(OPTION_AUDIO_RECORD_SOURCE, valueIfMissing);
    }

    /**
     * Returns the audio recording source.
     *
     * @return The stored value, if it exists in this configuration.
     * @throws IllegalArgumentException if the option does not exist in this configuration.
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public int getAudioRecordSource() {
        return getConfig().retrieveOption(OPTION_AUDIO_RECORD_SOURCE);
    }

    /**
     * Returns the audio minimum buffer size, in bytes.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or <code>valueIfMissing</code> if the value does not exist in this
     * configuration.
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public int getAudioMinBufferSize(int valueIfMissing) {
        return getConfig().retrieveOption(OPTION_AUDIO_MIN_BUFFER_SIZE, valueIfMissing);
    }

    /**
     * Returns the audio minimum buffer size, in bytes.
     *
     * @return The stored value, if it exists in this configuration.
     * @throws IllegalArgumentException if the option does not exist in this configuration.
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public int getAudioMinBufferSize() {
        return getConfig().retrieveOption(OPTION_AUDIO_MIN_BUFFER_SIZE);
    }

    /** Builder for a {@link VideoCaptureConfig}. */
    public static final class Builder implements
            UseCaseConfig.Builder<VideoCapture, VideoCaptureConfig, Builder>,
            ImageOutputConfig.Builder<VideoCaptureConfig, Builder>,
            CameraDeviceConfig.Builder<VideoCaptureConfig, Builder>,
            ThreadConfig.Builder<VideoCaptureConfig, Builder> {

        private final MutableOptionsBundle mMutableConfig;

        /** Creates a new Builder object. */
        public Builder() {
            this(MutableOptionsBundle.create());
        }

        private Builder(MutableOptionsBundle mutableConfig) {
            mMutableConfig = mutableConfig;

            Class<?> oldConfigClass =
                    mutableConfig.retrieveOption(TargetConfig.OPTION_TARGET_CLASS, null);
            if (oldConfigClass != null && !oldConfigClass.equals(VideoCapture.class)) {
                throw new IllegalArgumentException(
                        "Invalid target class configuration for "
                                + Builder.this
                                + ": "
                                + oldConfigClass);
            }

            setTargetClass(VideoCapture.class);
        }

        /**
         * Generates a Builder from another Config object
         *
         * @param configuration An immutable configuration to pre-populate this builder.
         * @return The new Builder.
         */
        public static Builder fromConfig(VideoCaptureConfig configuration) {
            return new Builder(MutableOptionsBundle.from(configuration));
        }

        /**
         * {@inheritDoc}
         *
         * @hide
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        public MutableConfig getMutableConfig() {
            return mMutableConfig;
        }

        /** The solution for the unchecked cast warning. */
        @Override
        public Builder builder() {
            return this;
        }

        @Override
        public VideoCaptureConfig build() {
            return new VideoCaptureConfig(OptionsBundle.from(mMutableConfig));
        }

        /**
         * Sets the recording frames per second.
         *
         * @param videoFrameRate The requested interval in seconds.
         * @return The current Builder.
         */
        public Builder setVideoFrameRate(int videoFrameRate) {
            getMutableConfig().insertOption(OPTION_VIDEO_FRAME_RATE, videoFrameRate);
            return builder();
        }

        /**
         * Sets the encoding bit rate.
         *
         * @param bitRate The requested bit rate in bits per second.
         * @return The current Builder.
         */
        public Builder setBitRate(int bitRate) {
            getMutableConfig().insertOption(OPTION_BIT_RATE, bitRate);
            return builder();
        }

        /**
         * Sets number of seconds between each key frame in seconds.
         *
         * @param interval The requested interval in seconds.
         * @return The current Builder.
         */
        public Builder setIFrameInterval(int interval) {
            getMutableConfig().insertOption(OPTION_INTRA_FRAME_INTERVAL, interval);
            return builder();
        }

        /**
         * Sets the bit rate of the audio stream.
         *
         * @param bitRate The requested bit rate in bits/s.
         * @return The current Builder.
         * @hide
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public Builder setAudioBitRate(int bitRate) {
            getMutableConfig().insertOption(OPTION_AUDIO_BIT_RATE, bitRate);
            return builder();
        }

        /**
         * Sets the sample rate of the audio stream.
         *
         * @param sampleRate The requested sample rate in bits/s.
         * @return The current Builder.
         * @hide
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public Builder setAudioSampleRate(int sampleRate) {
            getMutableConfig().insertOption(OPTION_AUDIO_SAMPLE_RATE, sampleRate);
            return builder();
        }

        /**
         * Sets the number of audio channels.
         *
         * @param channelCount The requested number of audio channels.
         * @return The current Builder.
         * @hide
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public Builder setAudioChannelCount(int channelCount) {
            getMutableConfig().insertOption(OPTION_AUDIO_CHANNEL_COUNT, channelCount);
            return builder();
        }

        /**
         * Sets the audio source.
         *
         * @param source The audio source. Currently only AudioSource.MIC is supported.
         * @return The current Builder.
         * @hide
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public Builder setAudioRecordSource(int source) {
            getMutableConfig().insertOption(OPTION_AUDIO_RECORD_SOURCE, source);
            return builder();
        }

        /**
         * Sets the audio min buffer size.
         *
         * @param minBufferSize The requested audio minimum buffer size, in bytes.
         * @return The current Builder.
         * @hide
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public Builder setAudioMinBufferSize(int minBufferSize) {
            getMutableConfig().insertOption(OPTION_AUDIO_MIN_BUFFER_SIZE, minBufferSize);
            return builder();
        }

        // Start of the default implementation of Config.Builder
        // *****************************************************************************************

        // Implementations of Config.Builder default methods

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        public <ValueT> Builder insertOption(Option<ValueT> opt, ValueT value) {
            getMutableConfig().insertOption(opt, value);
            return builder();
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        @Nullable
        public <ValueT> Builder removeOption(Option<ValueT> opt) {
            getMutableConfig().removeOption(opt);
            return builder();
        }

        // Implementations of TargetConfig.Builder default methods

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        public Builder setTargetClass(Class<VideoCapture> targetClass) {
            getMutableConfig().insertOption(OPTION_TARGET_CLASS, targetClass);

            // If no name is set yet, then generate a unique name
            if (null == getMutableConfig().retrieveOption(OPTION_TARGET_NAME, null)) {
                String targetName = targetClass.getCanonicalName() + "-" + UUID.randomUUID();
                setTargetName(targetName);
            }

            return builder();
        }

        @Override
        public Builder setTargetName(String targetName) {
            getMutableConfig().insertOption(OPTION_TARGET_NAME, targetName);
            return builder();
        }

        // Implementations of CameraDeviceConfig.Builder default methods

        @Override
        public Builder setLensFacing(CameraX.LensFacing lensFacing) {
            getMutableConfig().insertOption(OPTION_LENS_FACING, lensFacing);
            return builder();
        }

        // Implementations of ImageOutputConfig.Builder default methods

        @Override
        public Builder setTargetAspectRatio(Rational aspectRatio) {
            getMutableConfig().insertOption(OPTION_TARGET_ASPECT_RATIO, aspectRatio);
            return builder();
        }

        @Override
        public Builder setTargetRotation(@RotationValue int rotation) {
            getMutableConfig().insertOption(OPTION_TARGET_ROTATION, rotation);
            return builder();
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        public Builder setTargetResolution(Size resolution) {
            getMutableConfig().insertOption(OPTION_TARGET_RESOLUTION, resolution);
            return builder();
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        public Builder setMaxResolution(Size resolution) {
            getMutableConfig().insertOption(OPTION_MAX_RESOLUTION, resolution);
            return builder();
        }

        // Implementations of ThreadConfig.Builder default methods

        @Override
        public Builder setCallbackHandler(Handler handler) {
            getMutableConfig().insertOption(OPTION_CALLBACK_HANDLER, handler);
            return builder();
        }

        // Implementations of UseCaseConfig.Builder default methods

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        public Builder setDefaultSessionConfig(SessionConfig sessionConfig) {
            getMutableConfig().insertOption(OPTION_DEFAULT_SESSION_CONFIG, sessionConfig);
            return builder();
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        public Builder setOptionUnpacker(SessionConfig.OptionUnpacker optionUnpacker) {
            getMutableConfig().insertOption(OPTION_CONFIG_UNPACKER, optionUnpacker);
            return builder();
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        public Builder setSurfaceOccupancyPriority(int priority) {
            getMutableConfig().insertOption(OPTION_SURFACE_OCCUPANCY_PRIORITY, priority);
            return builder();
        }

        // End of the default implementation of Config.Builder
        // *****************************************************************************************
    }

    // Start of the default implementation of Config
    // *********************************************************************************************

    // Implementations of Config.Reader default methods

    /** @hide */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    public boolean containsOption(Option<?> id) {
        return getConfig().containsOption(id);
    }

    /** @hide */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    @Nullable
    public <ValueT> ValueT retrieveOption(Option<ValueT> id) {
        return getConfig().retrieveOption(id);
    }

    /** @hide */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    @Nullable
    public <ValueT> ValueT retrieveOption(Option<ValueT> id, @Nullable ValueT valueIfMissing) {
        return getConfig().retrieveOption(id, valueIfMissing);
    }

    /** @hide */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    public void findOptions(String idStem, OptionMatcher matcher) {
        getConfig().findOptions(idStem, matcher);
    }

    /** @hide */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    public Set<Option<?>> listOptions() {
        return getConfig().listOptions();
    }

    // Implementations of TargetConfig default methods

    @Override
    @Nullable
    public Class<VideoCapture> getTargetClass(@Nullable Class<VideoCapture> valueIfMissing) {
        @SuppressWarnings("unchecked") // Value should only be added via Builder#setTargetClass()
                Class<VideoCapture> storedClass =
                (Class<VideoCapture>) retrieveOption(
                        OPTION_TARGET_CLASS,
                        valueIfMissing);
        return storedClass;
    }

    @Override
    public Class<VideoCapture> getTargetClass() {
        @SuppressWarnings("unchecked") // Value should only be added via Builder#setTargetClass()
                Class<VideoCapture> storedClass =
                (Class<VideoCapture>) retrieveOption(
                        OPTION_TARGET_CLASS);
        return storedClass;
    }

    @Override
    @Nullable
    public String getTargetName(@Nullable String valueIfMissing) {
        return retrieveOption(OPTION_TARGET_NAME, valueIfMissing);
    }

    @Override
    public String getTargetName() {
        return retrieveOption(OPTION_TARGET_NAME);
    }

    // Implementations of CameraDeviceConfig default methods

    @Override
    @Nullable
    public CameraX.LensFacing getLensFacing(@Nullable CameraX.LensFacing valueIfMissing) {
        return retrieveOption(OPTION_LENS_FACING, valueIfMissing);
    }

    @Override
    public CameraX.LensFacing getLensFacing() {
        return retrieveOption(OPTION_LENS_FACING);
    }

    // Implementations of ImageOutputConfig default methods

    @Override
    @Nullable
    public Rational getTargetAspectRatio(@Nullable Rational valueIfMissing) {
        return retrieveOption(OPTION_TARGET_ASPECT_RATIO, valueIfMissing);
    }

    @Override
    public Rational getTargetAspectRatio() {
        return retrieveOption(OPTION_TARGET_ASPECT_RATIO);
    }

    @Override
    @RotationValue
    public int getTargetRotation(int valueIfMissing) {
        return retrieveOption(OPTION_TARGET_ROTATION, valueIfMissing);
    }

    @Override
    @RotationValue
    public int getTargetRotation() {
        return retrieveOption(OPTION_TARGET_ROTATION);
    }

    /** @hide */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    public Size getTargetResolution(Size valueIfMissing) {
        return retrieveOption(OPTION_TARGET_RESOLUTION, valueIfMissing);
    }

    /** @hide */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    public Size getTargetResolution() {
        return retrieveOption(OPTION_TARGET_RESOLUTION);
    }

    /** @hide */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    public Size getMaxResolution(Size valueIfMissing) {
        return retrieveOption(OPTION_MAX_RESOLUTION, valueIfMissing);
    }

    /** @hide */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    public Size getMaxResolution() {
        return retrieveOption(OPTION_MAX_RESOLUTION);
    }

    // Implementations of ThreadConfig default methods

    @Override
    @Nullable
    public Handler getCallbackHandler(@Nullable Handler valueIfMissing) {
        return retrieveOption(OPTION_CALLBACK_HANDLER, valueIfMissing);
    }

    @Override
    public Handler getCallbackHandler() {
        return retrieveOption(OPTION_CALLBACK_HANDLER);
    }

    // Implementations of UseCaseConfig default methods

    /** @hide */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    @Nullable
    public SessionConfig getDefaultSessionConfig(@Nullable SessionConfig valueIfMissing) {
        return retrieveOption(OPTION_DEFAULT_SESSION_CONFIG, valueIfMissing);
    }

    /** @hide */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    public SessionConfig getDefaultSessionConfig() {
        return retrieveOption(OPTION_DEFAULT_SESSION_CONFIG);
    }

    /** @hide */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    @Nullable
    public SessionConfig.OptionUnpacker getOptionUnpacker(
            @Nullable SessionConfig.OptionUnpacker valueIfMissing) {
        return retrieveOption(OPTION_CONFIG_UNPACKER, valueIfMissing);
    }

    /** @hide */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    public SessionConfig.OptionUnpacker getOptionUnpacker() {
        return retrieveOption(OPTION_CONFIG_UNPACKER);
    }

    /** @hide */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public int getSurfaceOccupancyPriority(int valueIfMissing) {
        return retrieveOption(OPTION_SURFACE_OCCUPANCY_PRIORITY, valueIfMissing);
    }

    /** @hide */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public int getSurfaceOccupancyPriority() {
        return retrieveOption(OPTION_SURFACE_OCCUPANCY_PRIORITY);
    }

    // End of the default implementation of Config
    // *********************************************************************************************

}
