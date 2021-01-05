package com.lhh.hcamera;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.lhh.hcamera.hcameraexception.HCameraPreviewListener;
import com.lhh.hcamera.hcameraexception.InvalidCameraFacingException;

import java.util.ArrayList;
import java.util.List;

public class HCamera {

    private static final String TAG = "HCamera";

    private Context mContext;
    private int mPreviewWidth;
    private int mPreviewHeight;
    private int mCameraFacing;
    private Handler mHandler;
    private List<Surface> mSurfaceList;
    private HCameraPreviewListener mPreviewListener;

    private CameraManager mCameraManager;
    private CameraDevice mCameraDevice;
    private CaptureRequest mCaptureRequest;
    private CameraCaptureSession mCaptureSession;
    private ImageReader mImageReader;

    private HCamera(Builder builder) {
        mContext = builder.mContext;
        mPreviewWidth = builder.mPreviewWidth;
        mPreviewHeight = builder.mPreviewHeight;
        mCameraFacing = builder.mCameraFacing;
        mHandler = builder.mHandler;
        mSurfaceList = builder.mSurfaceList;
        mPreviewListener = builder.mPreviewListener;

        mCameraManager = (CameraManager)mContext.getSystemService(Context.CAMERA_SERVICE);
        setUpImageReader();
    }

    public void open(final HCameraOpenCallback callback) throws InvalidCameraFacingException, SecurityException, CameraAccessException {
        String cameraId = getCameraId(mCameraFacing);
        if (cameraId == null) {
            throw new InvalidCameraFacingException();
        }
        if (!checkPermission()) throw new SecurityException();
        mCameraManager.openCamera(cameraId, new CameraDevice.StateCallback() {
            @Override
            public void onOpened(@NonNull CameraDevice camera) {
                Log.d(TAG, "onOpened: ");
                mCameraDevice = camera;
                if (callback != null) callback.onOpened();
            }

            @Override
            public void onDisconnected(@NonNull CameraDevice camera) {
                Log.w(TAG, "onDisconnected: ");
                if (callback != null) callback.onError(new Exception("camera disconnected"));
            }

            @Override
            public void onError(@NonNull CameraDevice camera, int error) {
                Log.e(TAG, "onError: ");
                if (callback != null) callback.onError(new Exception("camera error"));
            }
        }, mHandler);
    }

    public void close() {
        if (mImageReader != null) {
            mImageReader.close();
            mImageReader = null;
        }
    }

    public void startPreview() throws CameraAccessException {
        final CaptureRequest.Builder builder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        for (Surface surface: mSurfaceList)
            builder.addTarget(surface);
        mCameraDevice.createCaptureSession(mSurfaceList, new CameraCaptureSession.StateCallback() {
            @Override
            public void onConfigured(@NonNull CameraCaptureSession session) {
                try {
                    mCaptureRequest = builder.build();
                    mCaptureSession = session;
                    mCaptureSession.setRepeatingRequest(mCaptureRequest, null, null);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onConfigureFailed(@NonNull CameraCaptureSession session) {

            }
        }, mHandler);
    }

    private void setUpImageReader() {
        if (mImageReader == null) {
            mImageReader = ImageReader.newInstance(mPreviewWidth, mPreviewHeight, ImageFormat.YUV_420_888, 2);
            mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Log.d(TAG, "onImageAvailable: ");
                    Image image = reader.acquireLatestImage();
                    if (image != null) {
                        if (mPreviewListener != null)
                            mPreviewListener.onPreview(image);
                        image.close();
                    }
                }
            }, mHandler);
        }
        mSurfaceList.add(mImageReader.getSurface());
    }

    private boolean checkPermission() {
        return ContextCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    private String getCameraId(int facing) {
        try {
            for (String id: mCameraManager.getCameraIdList()) {
                CameraCharacteristics cameraCharacteristics
                        = mCameraManager.getCameraCharacteristics(id);
                if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == facing) {
                    StreamConfigurationMap map = cameraCharacteristics
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

    public static class Builder {

        private Context mContext;
        private int mPreviewWidth;
        private int mPreviewHeight;
        private int mCameraFacing = CameraCharacteristics.LENS_FACING_BACK;
        private Handler mHandler;
        private List<Surface> mSurfaceList = new ArrayList<>();
        private HCameraPreviewListener mPreviewListener;

        public Builder(Context context, int width, int height) {
            mContext = context;
            mPreviewWidth = width;
            mPreviewHeight = height;
        }

        public Builder previewListener(HCameraPreviewListener listener) {
            mPreviewListener = listener;
            return this;
        }

        public Builder facing(int facing) {
            mCameraFacing = facing;
            return this;
        }

        public Builder handler(Handler handler) {
            mHandler = handler;
            return this;
        }

        public Builder addSurface(Surface surface) {
            mSurfaceList.add(surface);
            return this;
        }

        public HCamera build() {
            return new HCamera(this);
        }
    }
}
