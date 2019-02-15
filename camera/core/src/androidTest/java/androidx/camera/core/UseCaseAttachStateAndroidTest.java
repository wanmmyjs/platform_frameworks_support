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

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.util.Size;
import android.view.Surface;

import androidx.camera.core.CameraX.LensFacing;
import androidx.camera.testing.fakes.FakeAppConfiguration;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

@RunWith(AndroidJUnit4.class)
public class UseCaseAttachStateAndroidTest {
    private final LensFacing mCameraLensFacing0 = LensFacing.BACK;
    private final LensFacing mCameraLensFacing1 = LensFacing.FRONT;
    private final CameraDevice mMockCameraDevice = Mockito.mock(CameraDevice.class);
    private final CameraCaptureSession mMockCameraCaptureSession =
            Mockito.mock(CameraCaptureSession.class);

    private String mCameraId;

    @Before
    public void setUp() {
        AppConfiguration appConfiguration = FakeAppConfiguration.create();
        CameraFactory cameraFactory = appConfiguration.getCameraFactory(/*valueIfMissing=*/ null);
        try {
            mCameraId = cameraFactory.cameraIdForLensFacing(LensFacing.BACK);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Unable to attach to camera with LensFacing " + LensFacing.BACK, e);
        }
        CameraX.init(ApplicationProvider.getApplicationContext(), appConfiguration);
    }

    @Test
    public void setSingleUseCaseOnline() {
        UseCaseAttachState useCaseAttachState = new UseCaseAttachState(mCameraId);
        FakeUseCaseConfiguration configuration =
                new FakeUseCaseConfiguration.Builder()
                        .setTargetName("UseCase")
                        .setLensFacing(mCameraLensFacing0)
                        .build();
        TestUseCase fakeUseCase = new TestUseCase(configuration, mCameraId);

        useCaseAttachState.setUseCaseOnline(fakeUseCase);

        SessionConfiguration.ValidatingBuilder builder = useCaseAttachState.getOnlineBuilder();
        SessionConfiguration sessionConfiguration = builder.build();
        assertThat(DeferrableSurfaces.surfaceList(sessionConfiguration.getSurfaces()))
                .containsExactly(fakeUseCase.mSurface);

        sessionConfiguration.getDeviceStateCallback().onOpened(mMockCameraDevice);
        verify(fakeUseCase.mDeviceStateCallback, times(1)).onOpened(mMockCameraDevice);

        sessionConfiguration.getSessionStateCallback().onConfigured(mMockCameraCaptureSession);
        verify(fakeUseCase.mSessionStateCallback, times(1)).onConfigured(mMockCameraCaptureSession);

        sessionConfiguration.getCameraCaptureCallback().onCaptureCompleted(null);
        verify(fakeUseCase.mCameraCaptureCallback, times(1)).onCaptureCompleted(null);
    }

    @Test
    public void setTwoUseCasesOnline() {
        UseCaseAttachState useCaseAttachState = new UseCaseAttachState(mCameraId);
        FakeUseCaseConfiguration configuration0 =
                new FakeUseCaseConfiguration.Builder()
                        .setTargetName("UseCase")
                        .setLensFacing(mCameraLensFacing0)
                        .build();
        TestUseCase fakeUseCase0 = new TestUseCase(configuration0, mCameraId);
        FakeUseCaseConfiguration configuration1 =
                new FakeUseCaseConfiguration.Builder()
                        .setTargetName("UseCase")
                        .setLensFacing(mCameraLensFacing0)
                        .build();
        TestUseCase fakeUseCase1 = new TestUseCase(configuration1, mCameraId);

        useCaseAttachState.setUseCaseOnline(fakeUseCase0);
        useCaseAttachState.setUseCaseOnline(fakeUseCase1);

        SessionConfiguration.ValidatingBuilder builder = useCaseAttachState.getOnlineBuilder();
        SessionConfiguration sessionConfiguration = builder.build();
        assertThat(DeferrableSurfaces.surfaceList(sessionConfiguration.getSurfaces()))
                .containsExactly(fakeUseCase0.mSurface, fakeUseCase1.mSurface);

        sessionConfiguration.getDeviceStateCallback().onOpened(mMockCameraDevice);
        verify(fakeUseCase0.mDeviceStateCallback, times(1)).onOpened(mMockCameraDevice);
        verify(fakeUseCase1.mDeviceStateCallback, times(1)).onOpened(mMockCameraDevice);

        sessionConfiguration.getSessionStateCallback().onConfigured(mMockCameraCaptureSession);
        verify(fakeUseCase0.mSessionStateCallback, times(1)).onConfigured(
                mMockCameraCaptureSession);
        verify(fakeUseCase1.mSessionStateCallback, times(1)).onConfigured(
                mMockCameraCaptureSession);

        sessionConfiguration.getCameraCaptureCallback().onCaptureCompleted(null);
        verify(fakeUseCase0.mCameraCaptureCallback, times(1)).onCaptureCompleted(null);
        verify(fakeUseCase1.mCameraCaptureCallback, times(1)).onCaptureCompleted(null);
    }

    @Test
    public void setUseCaseActiveOnly() {
        UseCaseAttachState useCaseAttachState = new UseCaseAttachState(mCameraId);
        FakeUseCaseConfiguration configuration =
                new FakeUseCaseConfiguration.Builder()
                        .setTargetName("UseCase")
                        .setLensFacing(mCameraLensFacing0)
                        .build();
        TestUseCase fakeUseCase = new TestUseCase(configuration, mCameraId);

        useCaseAttachState.setUseCaseActive(fakeUseCase);

        SessionConfiguration.ValidatingBuilder builder =
                useCaseAttachState.getActiveAndOnlineBuilder();
        SessionConfiguration sessionConfiguration = builder.build();
        assertThat(sessionConfiguration.getSurfaces()).isEmpty();

        sessionConfiguration.getDeviceStateCallback().onOpened(mMockCameraDevice);
        verify(fakeUseCase.mDeviceStateCallback, never()).onOpened(mMockCameraDevice);

        sessionConfiguration.getSessionStateCallback().onConfigured(mMockCameraCaptureSession);
        verify(fakeUseCase.mSessionStateCallback, never()).onConfigured(mMockCameraCaptureSession);

        sessionConfiguration.getCameraCaptureCallback().onCaptureCompleted(null);
        verify(fakeUseCase.mCameraCaptureCallback, never()).onCaptureCompleted(null);
    }

    @Test
    public void setUseCaseActiveAndOnline() {
        UseCaseAttachState useCaseAttachState = new UseCaseAttachState(mCameraId);
        FakeUseCaseConfiguration configuration =
                new FakeUseCaseConfiguration.Builder()
                        .setTargetName("UseCase")
                        .setLensFacing(mCameraLensFacing0)
                        .build();
        TestUseCase fakeUseCase = new TestUseCase(configuration, mCameraId);

        useCaseAttachState.setUseCaseOnline(fakeUseCase);
        useCaseAttachState.setUseCaseActive(fakeUseCase);

        SessionConfiguration.ValidatingBuilder builder =
                useCaseAttachState.getActiveAndOnlineBuilder();
        SessionConfiguration sessionConfiguration = builder.build();
        assertThat(DeferrableSurfaces.surfaceList(sessionConfiguration.getSurfaces()))
                .containsExactly(fakeUseCase.mSurface);

        sessionConfiguration.getDeviceStateCallback().onOpened(mMockCameraDevice);
        verify(fakeUseCase.mDeviceStateCallback, times(1)).onOpened(mMockCameraDevice);

        sessionConfiguration.getSessionStateCallback().onConfigured(mMockCameraCaptureSession);
        verify(fakeUseCase.mSessionStateCallback, times(1)).onConfigured(mMockCameraCaptureSession);

        sessionConfiguration.getCameraCaptureCallback().onCaptureCompleted(null);
        verify(fakeUseCase.mCameraCaptureCallback, times(1)).onCaptureCompleted(null);
    }

    @Test
    public void setUseCaseOffline() {
        UseCaseAttachState useCaseAttachState = new UseCaseAttachState(mCameraId);
        FakeUseCaseConfiguration configuration =
                new FakeUseCaseConfiguration.Builder()
                        .setTargetName("UseCase")
                        .setLensFacing(mCameraLensFacing0)
                        .build();
        TestUseCase fakeUseCase = new TestUseCase(configuration, mCameraId);

        useCaseAttachState.setUseCaseOnline(fakeUseCase);
        useCaseAttachState.setUseCaseOffline(fakeUseCase);

        SessionConfiguration.ValidatingBuilder builder = useCaseAttachState.getOnlineBuilder();
        SessionConfiguration sessionConfiguration = builder.build();
        assertThat(sessionConfiguration.getSurfaces()).isEmpty();

        sessionConfiguration.getDeviceStateCallback().onOpened(mMockCameraDevice);
        verify(fakeUseCase.mDeviceStateCallback, never()).onOpened(mMockCameraDevice);

        sessionConfiguration.getSessionStateCallback().onConfigured(mMockCameraCaptureSession);
        verify(fakeUseCase.mSessionStateCallback, never()).onConfigured(mMockCameraCaptureSession);

        sessionConfiguration.getCameraCaptureCallback().onCaptureCompleted(null);
        verify(fakeUseCase.mCameraCaptureCallback, never()).onCaptureCompleted(null);
    }

    @Test
    public void setUseCaseInactive() {
        UseCaseAttachState useCaseAttachState = new UseCaseAttachState(mCameraId);
        FakeUseCaseConfiguration configuration =
                new FakeUseCaseConfiguration.Builder()
                        .setTargetName("UseCase")
                        .setLensFacing(mCameraLensFacing0)
                        .build();
        TestUseCase fakeUseCase = new TestUseCase(configuration, mCameraId);

        useCaseAttachState.setUseCaseOnline(fakeUseCase);
        useCaseAttachState.setUseCaseActive(fakeUseCase);
        useCaseAttachState.setUseCaseInactive(fakeUseCase);

        SessionConfiguration.ValidatingBuilder builder =
                useCaseAttachState.getActiveAndOnlineBuilder();
        SessionConfiguration sessionConfiguration = builder.build();
        assertThat(sessionConfiguration.getSurfaces()).isEmpty();

        sessionConfiguration.getDeviceStateCallback().onOpened(mMockCameraDevice);
        verify(fakeUseCase.mDeviceStateCallback, never()).onOpened(mMockCameraDevice);

        sessionConfiguration.getSessionStateCallback().onConfigured(mMockCameraCaptureSession);
        verify(fakeUseCase.mSessionStateCallback, never()).onConfigured(mMockCameraCaptureSession);

        sessionConfiguration.getCameraCaptureCallback().onCaptureCompleted(null);
        verify(fakeUseCase.mCameraCaptureCallback, never()).onCaptureCompleted(null);
    }

    @Test
    public void updateUseCase() {
        UseCaseAttachState useCaseAttachState = new UseCaseAttachState(mCameraId);
        FakeUseCaseConfiguration configuration =
                new FakeUseCaseConfiguration.Builder()
                        .setTargetName("UseCase")
                        .setLensFacing(mCameraLensFacing0)
                        .build();
        TestUseCase fakeUseCase = new TestUseCase(configuration, mCameraId);

        useCaseAttachState.setUseCaseOnline(fakeUseCase);
        useCaseAttachState.setUseCaseActive(fakeUseCase);

        // The original template should be PREVIEW.
        SessionConfiguration firstSessionConfiguration =
                useCaseAttachState.getActiveAndOnlineBuilder().build();
        assertThat(firstSessionConfiguration.getTemplateType())
                .isEqualTo(CameraDevice.TEMPLATE_PREVIEW);

        // Change the template to STILL_CAPTURE.
        SessionConfiguration.Builder builder = new SessionConfiguration.Builder();
        builder.setTemplateType(CameraDevice.TEMPLATE_STILL_CAPTURE);
        fakeUseCase.attachToCamera(mCameraId, builder.build());

        useCaseAttachState.updateUseCase(fakeUseCase);

        // The new template should be STILL_CAPTURE.
        SessionConfiguration secondSessionConfiguration =
                useCaseAttachState.getActiveAndOnlineBuilder().build();
        assertThat(secondSessionConfiguration.getTemplateType())
                .isEqualTo(CameraDevice.TEMPLATE_STILL_CAPTURE);
    }

    @Test
    public void setUseCaseOnlineWithWrongCamera() {
        UseCaseAttachState useCaseAttachState = new UseCaseAttachState(mCameraId);
        FakeUseCaseConfiguration configuration =
                new FakeUseCaseConfiguration.Builder()
                        .setTargetName("UseCase")
                        .setLensFacing(mCameraLensFacing1)
                        .build();
        TestUseCase fakeUseCase = new TestUseCase(configuration, mCameraId);

        assertThrows(
                IllegalArgumentException.class,
                () -> useCaseAttachState.setUseCaseOnline(fakeUseCase));
    }

    @Test
    public void setUseCaseActiveWithWrongCamera() {
        UseCaseAttachState useCaseAttachState = new UseCaseAttachState(mCameraId);
        FakeUseCaseConfiguration configuration =
                new FakeUseCaseConfiguration.Builder()
                        .setTargetName("UseCase")
                        .setLensFacing(mCameraLensFacing1)
                        .build();
        TestUseCase fakeUseCase = new TestUseCase(configuration, mCameraId);

        assertThrows(
                IllegalArgumentException.class,
                () -> useCaseAttachState.setUseCaseActive(fakeUseCase));
    }

    private static class TestUseCase extends FakeUseCase {
        private final Surface mSurface = Mockito.mock(Surface.class);
        private final CameraDevice.StateCallback mDeviceStateCallback =
                Mockito.mock(CameraDevice.StateCallback.class);
        private final CameraCaptureSession.StateCallback mSessionStateCallback =
                Mockito.mock(CameraCaptureSession.StateCallback.class);
        private final CameraCaptureCallback mCameraCaptureCallback =
                Mockito.mock(CameraCaptureCallback.class);

        TestUseCase(FakeUseCaseConfiguration configuration, String cameraId) {
            super(configuration);
            Map<String, Size> suggestedResolutionMap = new HashMap<>();
            suggestedResolutionMap.put(cameraId, new Size(640, 480));
            updateSuggestedResolution(suggestedResolutionMap);
        }

        @Override
        protected Map<String, Size> onSuggestedResolutionUpdated(
                Map<String, Size> suggestedResolutionMap) {
            SessionConfiguration.Builder builder = new SessionConfiguration.Builder();
            builder.setTemplateType(CameraDevice.TEMPLATE_PREVIEW);
            builder.addSurface(new ImmediateSurface(mSurface));
            builder.setDeviceStateCallback(mDeviceStateCallback);
            builder.setSessionStateCallback(mSessionStateCallback);
            builder.setCameraCaptureCallback(mCameraCaptureCallback);

            LensFacing lensFacing =
                    ((CameraDeviceConfiguration) getUseCaseConfiguration()).getLensFacing();
            try {
                String cameraId = CameraX.getCameraWithLensFacing(lensFacing);
                attachToCamera(cameraId, builder.build());
            } catch (Exception e) {
                throw new IllegalArgumentException(
                        "Unable to attach to camera with LensFacing " + lensFacing, e);
            }
            return suggestedResolutionMap;
        }
    }
}