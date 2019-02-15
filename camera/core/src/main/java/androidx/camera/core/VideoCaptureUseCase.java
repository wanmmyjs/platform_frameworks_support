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

import android.location.Location;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.CamcorderProfile;
import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaCodecInfo;
import android.media.MediaCodecInfo.CodecCapabilities;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.MediaRecorder.AudioSource;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import android.util.Rational;
import android.util.Size;
import android.view.Display;
import android.view.Surface;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.camera.core.CameraX.LensFacing;
import androidx.camera.core.ImageOutputConfiguration.RotationValue;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A use case for taking a video.
 *
 * <p>This class is designed for simple video capturing. It gives basic configuration of the
 * recorded video such as resolution and file format.
 */
public class VideoCaptureUseCase extends BaseUseCase {

    /**
     * Provides a static configuration with implementation-agnostic options.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public static final Defaults DEFAULT_CONFIG = new Defaults();
    private static final Metadata EMPTY_METADATA = new Metadata();
    private static final String TAG = "VideoCaptureUseCase";
    /** Amount of time to wait for dequeuing a buffer from the videoEncoder. */
    private static final int DEQUE_TIMEOUT_USEC = 10000;
    /** Android preferred mime type for AVC video. */
    private static final String VIDEO_MIME_TYPE = "video/avc";
    private static final String AUDIO_MIME_TYPE = "audio/mp4a-latm";
    /** Camcorder profiles quality list */
    private static final int[] CamcorderQuality = {
            CamcorderProfile.QUALITY_2160P,
            CamcorderProfile.QUALITY_1080P,
            CamcorderProfile.QUALITY_720P,
            CamcorderProfile.QUALITY_480P
    };
    /**
     * Audio encoding
     *
     * <p>the result of PCM_8BIT and PCM_FLOAT are not good. Set PCM_16BIT as the first option.
     */
    private static final short[] sAudioEncoding = {
            AudioFormat.ENCODING_PCM_16BIT,
            AudioFormat.ENCODING_PCM_8BIT,
            AudioFormat.ENCODING_PCM_FLOAT
    };
    private final BufferInfo mVideoBufferInfo = new BufferInfo();
    private final Object mMuxerLock = new Object();
    /** Thread on which all encoding occurs. */
    private final HandlerThread mVideoHandlerThread =
            new HandlerThread(CameraXThreads.TAG + "video encoding thread");
    private final Handler mVideoHandler;
    /** Thread on which audio encoding occurs. */
    private final HandlerThread mAudioHandlerThread =
            new HandlerThread(CameraXThreads.TAG + "audio encoding thread");
    private final Handler mAudioHandler;
    private final AtomicBoolean mEndOfVideoStreamSignal = new AtomicBoolean(true);
    private final AtomicBoolean mEndOfAudioStreamSignal = new AtomicBoolean(true);
    private final AtomicBoolean mEndOfAudioVideoSignal = new AtomicBoolean(true);
    private final BufferInfo mAudioBufferInfo = new BufferInfo();
    /** For record the first sample written time. */
    private final AtomicBoolean mIsFirstVideoSampleWrite = new AtomicBoolean(false);
    private final AtomicBoolean mIsFirstAudioSampleWrite = new AtomicBoolean(false);
    private final VideoCaptureUseCaseConfiguration.Builder mUseCaseConfigBuilder;
    @NonNull
    private MediaCodec mVideoEncoder;
    @NonNull
    private MediaCodec mAudioEncoder;
    /** The muxer that writes the encoding data to file. */
    @GuardedBy("mMuxerLock")
    private MediaMuxer mMuxer;
    private boolean mMuxerStarted = false;
    /** The index of the video track used by the muxer. */
    private int mVideoTrackIndex;
    /** The index of the audio track used by the muxer. */
    private int mAudioTrackIndex;
    /** Surface the camera writes to, which the videoEncoder uses as input. */
    private Surface mCameraSurface;
    /** audio raw data */
    @NonNull
    private AudioRecord mAudioRecorder;
    private int mAudioBufferSize;
    private boolean mIsRecording = false;
    private int mAudioChannelCount;
    private int mAudioSampleRate;
    private int mAudioBitRate;

    /**
     * Creates a new video capture use case from the given configuration.
     *
     * @param configuration for this use case instance
     */
    public VideoCaptureUseCase(VideoCaptureUseCaseConfiguration configuration) {
        super(configuration);
        mUseCaseConfigBuilder = VideoCaptureUseCaseConfiguration.Builder.fromConfig(configuration);

        // video thread start
        mVideoHandlerThread.start();
        mVideoHandler = new Handler(mVideoHandlerThread.getLooper());

        // audio thread start
        mAudioHandlerThread.start();
        mAudioHandler = new Handler(mAudioHandlerThread.getLooper());
    }

    /** Creates a {@link MediaFormat} using parameters from the configuration */
    private static MediaFormat createMediaFormat(
            VideoCaptureUseCaseConfiguration configuration, Size resolution) {
        MediaFormat format =
                MediaFormat.createVideoFormat(
                        VIDEO_MIME_TYPE, resolution.getWidth(), resolution.getHeight());
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, configuration.getBitRate());
        format.setInteger(MediaFormat.KEY_FRAME_RATE, configuration.getVideoFrameRate());
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, configuration.getIFrameInterval());

        return format;
    }

    private static String getCameraIdUnchecked(LensFacing lensFacing) {
        try {
            return CameraX.getCameraWithLensFacing(lensFacing);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Unable to get camera id for camera lens facing " + lensFacing, e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @hide
     */
    @Override
    @Nullable
    @RestrictTo(Scope.LIBRARY_GROUP)
    protected UseCaseConfiguration.Builder<?, ?, ?> getDefaultBuilder() {
        VideoCaptureUseCaseConfiguration defaults =
                CameraX.getDefaultUseCaseConfiguration(VideoCaptureUseCaseConfiguration.class);
        if (defaults != null) {
            return VideoCaptureUseCaseConfiguration.Builder.fromConfig(defaults);
        }

        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @hide
     */
    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    protected Map<String, Size> onSuggestedResolutionUpdated(
            Map<String, Size> suggestedResolutionMap) {
        VideoCaptureUseCaseConfiguration configuration =
                (VideoCaptureUseCaseConfiguration) getUseCaseConfiguration();
        if (mCameraSurface != null) {
            mVideoEncoder.stop();
            mVideoEncoder.release();
            mAudioEncoder.stop();
            mAudioEncoder.release();
            mCameraSurface.release();
        }

        try {
            mVideoEncoder = MediaCodec.createEncoderByType(VIDEO_MIME_TYPE);
            mAudioEncoder = MediaCodec.createEncoderByType(AUDIO_MIME_TYPE);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create MediaCodec due to: " + e.getCause());
        }

        String cameraId = getCameraIdUnchecked(configuration.getLensFacing());
        Size resolution = suggestedResolutionMap.get(cameraId);
        if (resolution == null) {
            throw new IllegalArgumentException(
                    "Suggested resolution map missing resolution for camera " + cameraId);
        }

        setupEncoder(resolution);
        return suggestedResolutionMap;
    }

    /**
     * Starts recording video, which continues until {@link VideoCaptureUseCase#stopRecording()} is
     * called.
     *
     * <p>StartRecording() is asynchronous. User needs to check if any error occurs by setting the
     * {@link OnVideoSavedListener#onError(UseCaseError, String, Throwable)}.
     *
     * @param saveLocation Location to save the video capture
     * @param listener     Listener to call for the recorded video
     */
    public void startRecording(File saveLocation, OnVideoSavedListener listener) {
        mIsFirstVideoSampleWrite.set(false);
        mIsFirstAudioSampleWrite.set(false);
        startRecording(saveLocation, listener, EMPTY_METADATA);
    }

    /**
     * Starts recording video, which continues until {@link VideoCaptureUseCase#stopRecording()} is
     * called.
     *
     * <p>StartRecording() is asynchronous. User needs to check if any error occurs by setting the
     * {@link OnVideoSavedListener#onError(UseCaseError, String, Throwable)}.
     *
     * @param saveLocation Location to save the video capture
     * @param listener     Listener to call for the recorded video
     * @param metadata     Metadata to save with the recorded video
     */
    public void startRecording(
            File saveLocation, OnVideoSavedListener listener, Metadata metadata) {
        Log.i(TAG, "startRecording");

        if (!mEndOfAudioVideoSignal.get()) {
            listener.onError(
                    UseCaseError.RECORDING_IN_PROGRESS, "It is still in video recording!", null);
            return;
        }

        try {
            // audioRecord start
            mAudioRecorder.startRecording();
        } catch (IllegalStateException e) {
            listener.onError(UseCaseError.ENCODER_ERROR, "AudioRecorder start fail", e);
            return;
        }

        String cameraId =
                getCameraIdUnchecked(
                        ((CameraDeviceConfiguration) getUseCaseConfiguration()).getLensFacing());
        try {
            // video encoder start
            Log.i(TAG, "videoEncoder start");
            mVideoEncoder.start();
            // audio encoder start
            Log.i(TAG, "audioEncoder start");
            mAudioEncoder.start();

        } catch (IllegalStateException e) {
            setupEncoder(getAttachedSurfaceResolution(cameraId));
            listener.onError(UseCaseError.ENCODER_ERROR, "Audio/Video encoder start fail", e);
            return;
        }

        // Get the relative rotation or default to 0 if the camera info is unavailable
        int relativeRotation = 0;
        try {
            CameraInfo cameraInfo = CameraX.getCameraInfo(cameraId);
            relativeRotation =
                    cameraInfo.getSensorRotationDegrees(
                            ((ImageOutputConfiguration) getUseCaseConfiguration())
                                    .getTargetRotation(Surface.ROTATION_0));
        } catch (CameraInfoUnavailableException e) {
            Log.e(TAG, "Unable to retrieve camera sensor orientation.", e);
        }

        try {
            synchronized (mMuxerLock) {
                mMuxer =
                        new MediaMuxer(
                                saveLocation.getAbsolutePath(),
                                MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

                mMuxer.setOrientationHint(relativeRotation);
                if (metadata.location != null) {
                    mMuxer.setLocation(
                            (float) metadata.location.getLatitude(),
                            (float) metadata.location.getLongitude());
                }
            }
        } catch (IOException e) {
            setupEncoder(getAttachedSurfaceResolution(cameraId));
            listener.onError(UseCaseError.MUXER_ERROR, "MediaMuxer creation failed!", e);
            return;
        }

        mEndOfVideoStreamSignal.set(false);
        mEndOfAudioStreamSignal.set(false);
        mEndOfAudioVideoSignal.set(false);
        mIsRecording = true;

        notifyActive();
        mAudioHandler.post(
                () -> {
                    audioEncode(listener);
                });

        mVideoHandler.post(
                () -> {
                    boolean errorOccurred = videoEncode(listener);
                    if (!errorOccurred) {
                        listener.onVideoSaved(saveLocation);
                    }
                });
    }

    /**
     * Stops recording video, this must be called after {@link
     * VideoCaptureUseCase#startRecording(File, OnVideoSavedListener, Metadata)} is called.
     *
     * <p>stopRecording() is asynchronous API. User need to check if {@link
     * OnVideoSavedListener#onVideoSaved(File)} or {@link OnVideoSavedListener#onError(UseCaseError,
     * String, Throwable)} be called before startRecording.
     */
    public void stopRecording() {
        Log.i(TAG, "stopRecording");
        notifyInactive();
        if (!mEndOfAudioVideoSignal.get() && mIsRecording) {
            // stop audio encoder thread, and wait video encoder and muxer stop.
            mEndOfAudioStreamSignal.set(true);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    public void clear() {
        mVideoHandlerThread.quitSafely();

        if (mVideoEncoder != null) {
            mVideoEncoder.release();
            mVideoEncoder = null;
        }

        // audio encoder release
        mAudioHandlerThread.quitSafely();
        if (mAudioEncoder != null) {
            mAudioEncoder.release();
            mAudioEncoder = null;
        }

        if (mAudioRecorder != null) {
            mAudioRecorder.release();
            mAudioRecorder = null;
        }

        if (mCameraSurface != null) {
            mCameraSurface.release();
            mCameraSurface = null;
        }
        super.clear();
    }

    /**
     * Sets the desired rotation of the output video.
     *
     * <p>In most cases this should be set to the current rotation returned by {@link
     * Display#getRotation()}.
     *
     * @param rotation Desired rotation of the output video.
     */
    public void setTargetRotation(@RotationValue int rotation) {
        ImageOutputConfiguration oldconfig = (ImageOutputConfiguration) getUseCaseConfiguration();
        int oldRotation = oldconfig.getTargetRotation(ImageOutputConfiguration.INVALID_ROTATION);
        if (oldRotation == ImageOutputConfiguration.INVALID_ROTATION || oldRotation != rotation) {
            mUseCaseConfigBuilder.setTargetRotation(rotation);
            updateUseCaseConfiguration(mUseCaseConfigBuilder.build());

            // TODO(b/122846516): Update session configuration and possibly reconfigure session.
        }
    }

    /**
     * Setup the {@link MediaCodec} for encoding video from a camera {@link Surface} and encoding
     * audio from selected audio source.
     */
    private void setupEncoder(Size resolution) {
        VideoCaptureUseCaseConfiguration configuration =
                (VideoCaptureUseCaseConfiguration) getUseCaseConfiguration();

        // video encoder setup
        mVideoEncoder.reset();
        mVideoEncoder.configure(
                createMediaFormat(configuration, resolution), /*surface*/
                null, /*crypto*/
                null,
                MediaCodec.CONFIGURE_FLAG_ENCODE);
        if (mCameraSurface != null) {
            mCameraSurface.release();
        }
        mCameraSurface = mVideoEncoder.createInputSurface();

        SessionConfiguration.Builder builder =
                SessionConfiguration.Builder.createFrom(configuration);
        builder.addSurface(new ImmediateSurface(mCameraSurface));

        String cameraId = getCameraIdUnchecked(configuration.getLensFacing());
        attachToCamera(cameraId, builder.build());

        // audio encoder setup
        setAudioParametersByCamcorderProfile(resolution, cameraId);
        mAudioEncoder.reset();
        mAudioEncoder.configure(
                createAudioMediaFormat(), null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

        if (mAudioRecorder != null) {
            mAudioRecorder.release();
        }
        mAudioRecorder = autoConfigAudioRecordSource(configuration);
        // check mAudioRecorder
        if (mAudioRecorder == null) {
            Log.e(TAG, "AudioRecord object cannot initialized correctly!");
        }

        mVideoTrackIndex = -1;
        mAudioTrackIndex = -1;
        mIsRecording = false;
    }

    /**
     * Write a buffer that has been encoded to file.
     *
     * @param bufferIndex the index of the buffer in the videoEncoder that has available data
     * @return returns true if this buffer is the end of the stream
     */
    private boolean writeVideoEncodedBuffer(int bufferIndex) {
        if (bufferIndex < 0) {
            Log.e(TAG, "Output buffer should not have negative index: " + bufferIndex);
            return false;
        }
        // Get data from buffer
        ByteBuffer outputBuffer = mVideoEncoder.getOutputBuffer(bufferIndex);

        // Check if buffer is valid, if not then return
        if (outputBuffer == null) {
            Log.d(TAG, "OutputBuffer was null.");
            return false;
        }

        // Write data to mMuxer if available
        if (mAudioTrackIndex >= 0 && mVideoTrackIndex >= 0 && mVideoBufferInfo.size > 0) {
            outputBuffer.position(mVideoBufferInfo.offset);
            outputBuffer.limit(mVideoBufferInfo.offset + mVideoBufferInfo.size);
            mVideoBufferInfo.presentationTimeUs = (System.nanoTime() / 1000);

            synchronized (mMuxerLock) {
                if (!mIsFirstVideoSampleWrite.get()) {
                    Log.i(TAG, "First video sample written.");
                    mIsFirstVideoSampleWrite.set(true);
                }
                mMuxer.writeSampleData(mVideoTrackIndex, outputBuffer, mVideoBufferInfo);
            }
        }

        // Release data
        mVideoEncoder.releaseOutputBuffer(bufferIndex, false);

        // Return true if EOS is set
        return (mVideoBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
    }

    private boolean writeAudioEncodedBuffer(int bufferIndex) {
        ByteBuffer buffer = getOutputBuffer(mAudioEncoder, bufferIndex);
        buffer.position(mAudioBufferInfo.offset);
        if (mAudioTrackIndex >= 0
                && mVideoTrackIndex >= 0
                && mAudioBufferInfo.size > 0
                && mAudioBufferInfo.presentationTimeUs > 0) {
            try {
                synchronized (mMuxerLock) {
                    if (!mIsFirstAudioSampleWrite.get()) {
                        Log.i(TAG, "First audio sample written.");
                        mIsFirstAudioSampleWrite.set(true);
                    }
                    mMuxer.writeSampleData(mAudioTrackIndex, buffer, mAudioBufferInfo);
                }
            } catch (Exception e) {
                Log.e(
                        TAG,
                        "audio error:size="
                                + mAudioBufferInfo.size
                                + "/offset="
                                + mAudioBufferInfo.offset
                                + "/timeUs="
                                + mAudioBufferInfo.presentationTimeUs);
                e.printStackTrace();
            }
        }
        mAudioEncoder.releaseOutputBuffer(bufferIndex, false);
        return (mAudioBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
    }

    /**
     * Encoding which runs indefinitely until end of stream is signaled. This should not run on the
     * main thread otherwise it will cause the application to block.
     *
     * @return returns {@code true} if an error condition occurred, otherwise returns {@code false}
     */
    private boolean videoEncode(OnVideoSavedListener videoSavedListener) {
        VideoCaptureUseCaseConfiguration configuration =
                (VideoCaptureUseCaseConfiguration) getUseCaseConfiguration();
        // Main encoding loop. Exits on end of stream.
        boolean errorOccurred = false;
        boolean videoEos = false;
        while (!videoEos && !errorOccurred) {
            // Check for end of stream from main thread
            if (mEndOfVideoStreamSignal.get()) {
                mVideoEncoder.signalEndOfInputStream();
                mEndOfVideoStreamSignal.set(false);
            }

            // Deque buffer to check for processing step
            int outputBufferId =
                    mVideoEncoder.dequeueOutputBuffer(mVideoBufferInfo, DEQUE_TIMEOUT_USEC);
            switch (outputBufferId) {
                case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                    if (mMuxerStarted) {
                        videoSavedListener.onError(
                                UseCaseError.ENCODER_ERROR,
                                "Unexpected change in video encoding format.",
                                null);
                        errorOccurred = true;
                    }

                    synchronized (mMuxerLock) {
                        mVideoTrackIndex = mMuxer.addTrack(mVideoEncoder.getOutputFormat());
                        if (mAudioTrackIndex >= 0 && mVideoTrackIndex >= 0) {
                            mMuxerStarted = true;
                            Log.i(TAG, "media mMuxer start");
                            mMuxer.start();
                        }
                    }
                    break;
                case MediaCodec.INFO_TRY_AGAIN_LATER:
                    // Timed out. Just wait until next attempt to deque.
                case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                    // Ignore output buffers changed since we dequeue a single buffer instead of
                    // multiple
                    break;
                default:
                    videoEos = writeVideoEncodedBuffer(outputBufferId);
            }
        }

        try {
            Log.i(TAG, "videoEncoder stop");
            mVideoEncoder.stop();
        } catch (IllegalStateException e) {
            videoSavedListener.onError(UseCaseError.ENCODER_ERROR, "Video encoder stop failed!", e);
            errorOccurred = true;
        }

        try {
            // new MediaMuxer instance required for each new file written, and release current one.
            synchronized (mMuxerLock) {
                if (mMuxer != null) {
                    if (mMuxerStarted) {
                        mMuxer.stop();
                    }
                    mMuxer.release();
                    mMuxer = null;
                }
            }
        } catch (IllegalStateException e) {
            videoSavedListener.onError(UseCaseError.MUXER_ERROR, "Muxer stop failed!", e);
            errorOccurred = true;
        }

        mMuxerStarted = false;
        // Do the setup of the videoEncoder at the end of video recording instead of at the start of
        // recording because it requires attaching a new Surface. This causes a glitch so we don't
        // want
        // that to incur latency at the start of capture.
        setupEncoder(
                getAttachedSurfaceResolution(getCameraIdUnchecked(configuration.getLensFacing())));
        notifyReset();

        // notify the UI thread that the video recording has finished
        mEndOfAudioVideoSignal.set(true);

        Log.i(TAG, "Video encode thread end.");
        return errorOccurred;
    }

    private boolean audioEncode(OnVideoSavedListener videoSavedListener) {
        // Audio encoding loop. Exits on end of stream.
        boolean audioEos = false;
        int outIndex;
        while (!audioEos && mIsRecording) {
            // Check for end of stream from main thread
            if (mEndOfAudioStreamSignal.get()) {
                mEndOfAudioStreamSignal.set(false);
                mIsRecording = false;
            }

            // get audio deque input buffer
            if (mAudioEncoder != null && mAudioRecorder != null) {
                int index = mAudioEncoder.dequeueInputBuffer(-1);
                if (index >= 0) {
                    final ByteBuffer buffer = getInputBuffer(mAudioEncoder, index);
                    buffer.clear();
                    int length = mAudioRecorder.read(buffer, mAudioBufferSize);
                    if (length > 0) {
                        mAudioEncoder.queueInputBuffer(
                                index,
                                0,
                                length,
                                (System.nanoTime() / 1000),
                                mIsRecording ? 0 : MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    }
                }

                // start to dequeue audio output buffer
                do {
                    outIndex = mAudioEncoder.dequeueOutputBuffer(mAudioBufferInfo, 0);
                    switch (outIndex) {
                        case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                            synchronized (mMuxerLock) {
                                mAudioTrackIndex = mMuxer.addTrack(mAudioEncoder.getOutputFormat());
                                if (mAudioTrackIndex >= 0 && mVideoTrackIndex >= 0) {
                                    mMuxerStarted = true;
                                    mMuxer.start();
                                }
                            }
                            break;
                        case MediaCodec.INFO_TRY_AGAIN_LATER:
                            break;
                        default:
                            audioEos = writeAudioEncodedBuffer(outIndex);
                    }
                } while (outIndex >= 0 && !audioEos); // end of dequeue output buffer
            }
        } // end of while loop

        // Audio Stop
        try {
            Log.i(TAG, "audioRecorder stop");
            mAudioRecorder.stop();
        } catch (IllegalStateException e) {
            videoSavedListener.onError(
                    UseCaseError.ENCODER_ERROR, "Audio recorder stop failed!", e);
        }

        try {
            mAudioEncoder.stop();
        } catch (IllegalStateException e) {
            videoSavedListener.onError(UseCaseError.ENCODER_ERROR, "Audio encoder stop failed!", e);
        }

        Log.i(TAG, "Audio encode thread end");
        // Use AtomicBoolean to signal because MediaCodec.signalEndOfInputStream() is not thread
        // safe
        mEndOfVideoStreamSignal.set(true);

        return false;
    }

    private ByteBuffer getInputBuffer(MediaCodec codec, int index) {
        return codec.getInputBuffer(index);
    }

    private ByteBuffer getOutputBuffer(MediaCodec codec, int index) {
        return codec.getOutputBuffer(index);
    }

    /** Creates a {@link MediaFormat} using parameters for audio from the configuration */
    private MediaFormat createAudioMediaFormat() {
        MediaFormat format =
                MediaFormat.createAudioFormat(AUDIO_MIME_TYPE, mAudioSampleRate,
                        mAudioChannelCount);
        format.setInteger(
                MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        format.setInteger(MediaFormat.KEY_BIT_RATE, mAudioBitRate);

        return format;
    }

    /** Create a AudioRecord object to get raw data */
    private AudioRecord autoConfigAudioRecordSource(
            VideoCaptureUseCaseConfiguration configuration) {
        for (short audioFormat : sAudioEncoding) {

            // Use channel count to determine stereo vs mono
            int channelConfig =
                    mAudioChannelCount == 1
                            ? AudioFormat.CHANNEL_IN_MONO
                            : AudioFormat.CHANNEL_IN_STEREO;
            int source = configuration.getAudioRecordSource();

            try {
                int bufferSize =
                        AudioRecord.getMinBufferSize(mAudioSampleRate, channelConfig, audioFormat);

                if (bufferSize <= 0) {
                    bufferSize = configuration.getAudioMinBufferSize();
                }

                AudioRecord recorder =
                        new AudioRecord(
                                source,
                                mAudioSampleRate,
                                channelConfig,
                                audioFormat,
                                bufferSize * 2);

                if (recorder.getState() == AudioRecord.STATE_INITIALIZED) {
                    mAudioBufferSize = bufferSize;
                    Log.i(
                            TAG,
                            "source: "
                                    + source
                                    + " audioSampleRate: "
                                    + mAudioSampleRate
                                    + " channelConfig: "
                                    + channelConfig
                                    + " audioFormat: "
                                    + audioFormat
                                    + " bufferSize: "
                                    + bufferSize);
                    return recorder;
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception, keep trying.", e);
            }
        }

        return null;
    }

    /** Set audio record parameters by CamcorderProfile */
    private void setAudioParametersByCamcorderProfile(Size currentResolution, String cameraId) {
        CamcorderProfile profile;
        boolean isCamcorderProfileFound = false;

        for (int quality : CamcorderQuality) {
            if (CamcorderProfile.hasProfile(Integer.parseInt(cameraId), quality)) {
                profile = CamcorderProfile.get(Integer.parseInt(cameraId), quality);
                if (currentResolution.getWidth() == profile.videoFrameWidth
                        && currentResolution.getHeight() == profile.videoFrameHeight) {
                    mAudioChannelCount = profile.audioChannels;
                    mAudioSampleRate = profile.audioSampleRate;
                    mAudioBitRate = profile.audioBitRate;
                    isCamcorderProfileFound = true;
                    break;
                }
            }
        }

        // In case no corresponding camcorder profile can be founded, * get default value from
        // VideoCaptureUseCaseConfiguration.
        if (!isCamcorderProfileFound) {
            VideoCaptureUseCaseConfiguration config =
                    (VideoCaptureUseCaseConfiguration) getUseCaseConfiguration();
            mAudioChannelCount = config.getAudioChannelCount();
            mAudioSampleRate = config.getAudioSampleRate();
            mAudioBitRate = config.getAudioBitRate();
        }
    }

    /**
     * Describes the error that occurred during video capture operations.
     *
     * <p>This is a parameter sent to the error callback functions set in listeners such as {@link
     * VideoCaptureUseCase.OnVideoSavedListener.onError}.
     *
     * <p>See message parameter in onError callback or log for more details.
     */
    public enum UseCaseError {
        /**
         * An unknown error occurred.
         *
         * <p>See message parameter in onError callback or log for more details.
         */
        UNKNOWN_ERROR,
        /**
         * An error occurred with encoder state, either when trying to change state or when an
         * unexpected state change occurred.
         */
        ENCODER_ERROR,
        /** An error with muxer state such as during creation or when stopping. */
        MUXER_ERROR,
        /**
         * An error indicating start recording was called when video recording is still in progress.
         */
        RECORDING_IN_PROGRESS
    }

    /** Listener containing callbacks for video file I/O events. */
    public interface OnVideoSavedListener {
        /** Called when the video has been successfully saved. */
        void onVideoSaved(File file);

        /** Called when an error occurs while attempting to save the video. */
        void onError(UseCaseError useCaseError, String message, @Nullable Throwable cause);
    }

    /**
     * Provides a base static default configuration for the VideoCaptureUseCase
     *
     * <p>These values may be overridden by the implementation. They only provide a minimum set of
     * defaults that are implementation independent.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public static final class Defaults
            implements ConfigurationProvider<VideoCaptureUseCaseConfiguration> {
        private static final Handler DEFAULT_HANDLER = new Handler(Looper.getMainLooper());
        private static final Rational DEFAULT_ASPECT_RATIO = new Rational(16, 9);
        private static final int DEFAULT_VIDEO_FRAME_RATE = 30;
        /** 8Mb/s the recommend rate for 30fps 1080p */
        private static final int DEFAULT_BIT_RATE = 8 * 1024 * 1024;
        /** Seconds between each key frame */
        private static final int DEFAULT_INTRA_FRAME_INTERVAL = 1;
        /** audio bit rate */
        private static final int DEFAULT_AUDIO_BIT_RATE = 64000;
        /** audio sample rate */
        private static final int DEFAULT_AUDIO_SAMPLE_RATE = 8000;
        /** audio channel count */
        private static final int DEFAULT_AUDIO_CHANNEL_COUNT = 1;
        /** audio record source */
        private static final int DEFAULT_AUDIO_RECORD_SOURCE = AudioSource.MIC;
        /** audio default minimum buffer size */
        private static final int DEFAULT_AUDIO_MIN_BUFFER_SIZE = 1024;
        /** Current max resolution of VideoCaptureUseCase is set as FHD */
        private static final Size DEFAULT_MAX_RESOLUTION = new Size(1920, 1080);
        /** Surface occupancy prioirty to this use case */
        private static final int DEFAULT_SURFACE_OCCUPANCY_PRIORITY = 3;

        private static final VideoCaptureUseCaseConfiguration DEFAULT_CONFIG;

        static {
            VideoCaptureUseCaseConfiguration.Builder builder =
                    new VideoCaptureUseCaseConfiguration.Builder()
                            .setCallbackHandler(DEFAULT_HANDLER)
                            .setTargetAspectRatio(DEFAULT_ASPECT_RATIO)
                            .setVideoFrameRate(DEFAULT_VIDEO_FRAME_RATE)
                            .setBitRate(DEFAULT_BIT_RATE)
                            .setIFrameInterval(DEFAULT_INTRA_FRAME_INTERVAL)
                            .setAudioBitRate(DEFAULT_AUDIO_BIT_RATE)
                            .setAudioSampleRate(DEFAULT_AUDIO_SAMPLE_RATE)
                            .setAudioChannelCount(DEFAULT_AUDIO_CHANNEL_COUNT)
                            .setAudioRecordSource(DEFAULT_AUDIO_RECORD_SOURCE)
                            .setAudioMinBufferSize(DEFAULT_AUDIO_MIN_BUFFER_SIZE)
                            .setMaxResolution(DEFAULT_MAX_RESOLUTION)
                            .setSurfaceOccupancyPriority(DEFAULT_SURFACE_OCCUPANCY_PRIORITY);

            DEFAULT_CONFIG = builder.build();
        }

        @Override
        public VideoCaptureUseCaseConfiguration getConfiguration() {
            return DEFAULT_CONFIG;
        }
    }

    /** Holder class for metadata that should be saved alongside captured video. */
    public static final class Metadata {
        /** Data representing a geographic location. */
        public @Nullable Location location;
    }
}