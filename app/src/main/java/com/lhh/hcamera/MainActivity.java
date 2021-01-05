package com.lhh.hcamera;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private TextureView textureView;

    private CameraManager cameraManager;
    private CameraDevice cameraDevice;
    private ImageReader imageReader;
    private CaptureRequest captureRequest;
    private CameraCaptureSession captureSession;
    private Handler handler;
    private int previewWidth;
    private int previewHeight;

    private HCamera hCamera;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        initView();
        startActivity(new Intent(this, HCameraActivity.class));
        finish();
    }

    private void initView() {
        textureView = findViewById(R.id.texture_view);
        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
                previewWidth = width;
                previewHeight = height;
                initCamera();
            }

            @Override
            public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {

            }
        });
    }

    private boolean checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) !=
                PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 0);
            return false;
        }
        return true;
    }

    private void initCamera() {
        String cameraId = setUpCamera();
        if (cameraId != null) {
            openCamera(cameraId);
        }
    }

    private String setUpCamera() {
        HandlerThread handlerThread = new HandlerThread("camera-background");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
        cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
            for (String id: cameraManager.getCameraIdList()) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(id);
                if (characteristics.get(CameraCharacteristics.LENS_FACING)
                        == CameraCharacteristics.LENS_FACING_FRONT) {
                    StreamConfigurationMap map = characteristics
                            .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                    if (map != null) {
                        return id;
                    }
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void openCamera(String cameraId) {
        try {
            if (!checkCameraPermission()) return;
            cameraManager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    Log.d(TAG, "onOpened: ");
                    cameraDevice = camera;
                    startPreview();
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    Log.w(TAG, "onDisconnected: ");
                    if(imageReader != null)
                        imageReader.close();
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    Log.d(TAG, "onError: ");
                }
            }, handler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void startPreview() {
        setUpImageSurface();
        SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
        surfaceTexture.setDefaultBufferSize(previewWidth, previewHeight);
        Surface previewSurface = new Surface(surfaceTexture);
        Surface imageReaderSurface = imageReader.getSurface();
        try {
            final CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            builder.addTarget(previewSurface);
            builder.addTarget(imageReaderSurface);
            cameraDevice.createCaptureSession(Arrays.asList(previewSurface, imageReaderSurface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    try {
                        captureRequest = builder.build();
                        captureSession = session;
                        captureSession.setRepeatingRequest(captureRequest, captureCallback, null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                }
            }, handler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void setUpImageSurface() {
        imageReader = ImageReader.newInstance(previewWidth, previewHeight, ImageFormat.YUV_420_888, 2);
        imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                Log.d(TAG, "onImageAvailable: ");
                Image image = reader.acquireLatestImage();
                image.close();
            }
        }, handler);
    }

    private CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
            super.onCaptureStarted(session, request, timestamp, frameNumber);
        }

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
            super.onCaptureProgressed(session, request, partialResult);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
        }

        @Override
        public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
            super.onCaptureFailed(session, request, failure);
        }

        @Override
        public void onCaptureSequenceCompleted(@NonNull CameraCaptureSession session, int sequenceId, long frameNumber) {
            super.onCaptureSequenceCompleted(session, sequenceId, frameNumber);
        }

        @Override
        public void onCaptureSequenceAborted(@NonNull CameraCaptureSession session, int sequenceId) {
            super.onCaptureSequenceAborted(session, sequenceId);
        }

        @Override
        public void onCaptureBufferLost(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull Surface target, long frameNumber) {
            super.onCaptureBufferLost(session, request, target, frameNumber);
        }
    };
}