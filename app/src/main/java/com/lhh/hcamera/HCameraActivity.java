package com.lhh.hcamera;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.Surface;
import android.view.TextureView;

import com.lhh.hcamera.hcameraexception.HCameraPreviewListener;

public class HCameraActivity extends AppCompatActivity implements HCameraPreviewListener {

    private TextureView textureView;

    private HCamera mHCamera;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_h_camera);

        initView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mHCamera != null) {
            mHCamera.close();
            mHCamera = null;
        }
    }

    private void initView() {
        textureView = findViewById(R.id.texture_view);
        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
                initCamera(width, height);
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

    private void initCamera(int width, int height) {
        SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
        surfaceTexture.setDefaultBufferSize(width, height);
        HandlerThread handlerThread = new HandlerThread("camera-background");
        handlerThread.start();
        mHCamera = new HCamera.Builder(this, width, height)
                .facing(CameraCharacteristics.LENS_FACING_FRONT)
                .addSurface(new Surface(surfaceTexture))
                .handler(new Handler(handlerThread.getLooper()))
                .previewListener(this)
                .build();
        try {
            mHCamera.open(new HCameraOpenCallback() {
                @Override
                public void onOpened() {
                    try {
                        mHCamera.startPreview();
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onError(Exception e) {

                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPreview(Image image) {

    }
}