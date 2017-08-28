package com.tangxb.candid;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

/**
 * 定义一个预览类
 */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private static final String TAG = CameraPreview.class.getSimpleName();
    /**
     * 前置摄像头
     */
    private final int cameraType = Camera.CameraInfo.CAMERA_FACING_BACK;
    /**
     * 旋转角度
     */
    private final int displayOrientation = 90;
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private boolean cameraConfigured;
    private SurfaceCreatedListener surfaceCreatedListener;
    /**
     * 相机是否出现错误
     */
    private boolean cameraError;

    public interface SurfaceCreatedListener {
        void surfaceCreated();
    }

    public void setSurfaceCreatedListener(SurfaceCreatedListener surfaceCreatedListener) {
        this.surfaceCreatedListener = surfaceCreatedListener;
    }

    public boolean isSurfaceCreated() {
        return cameraConfigured;
    }

    public CameraPreview(Context context) {
        super(context);
        mCamera = initCamera();
        // 通过SurfaceView获得SurfaceHolder
        mHolder = getHolder();
        // 为SurfaceHolder指定回调
        mHolder.addCallback(this);
        // 设置Surface不维护自己的缓冲区，而是等待屏幕的渲染引擎将内容推送到界面 在Android3.0之后弃用
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // no-op -- wait until surfaceChanged()
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // Surface发生改变的时候将被调用，第一次显示到界面的时候也会被调用
        if (mHolder.getSurface() == null) {
            // 如果Surface为空，不继续操作
            return;
        }

        // 停止Camera的预览
        try {
            mCamera.stopPreview();
        } catch (Exception e) {
            Log.d(TAG, "当Surface改变后，停止预览出错");
        }

        // 在预览前可以指定Camera的各项参数

        // 重新开始预览
        try {
            initPreview(width, height);
            startPreview();
        } catch (Exception e) {
            cameraError = true;
            Log.d(TAG, "预览Camera出错");
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        cameraConfigured = false;
        releaseCamera();
    }

    /**
     * 检测设备是否存在Camera硬件
     */
    private boolean checkCameraHardware() {
        if (getContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            // 存在
            return true;
        } else {
            // 不存在
            return false;
        }
    }

    public void releaseCamera() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    public boolean isCameraError() {
        return cameraError;
    }

    public Camera getCamera() {
        return mCamera;
    }

    /**
     * 打开一个Camera
     */
    public Camera initCamera() {
        if (checkCameraHardware()) {
            Camera camera = null;
            try {
                camera = Camera.open(cameraType);
                camera.setDisplayOrientation(displayOrientation);
            } catch (Exception e) {
                Log.d(TAG, "打开Camera失败,请重新检查");
            }
            return camera;
        }
        return null;
    }

    /**
     * parameters.setPreviewSize
     * parameters.setPictureSize
     * 以上2个方法需要特别注意一下,如果出错请用parameters.getSupportedPictureSizes查看相机支持的分辨率
     *
     * @param width
     * @param height
     */
    private void initPreview(int width, int height) {
        if (mCamera != null && mHolder != null) {
            try {
                mCamera.setPreviewDisplay(mHolder);
            } catch (Throwable t) {
                Log.e(TAG, "Exception in setPreviewDisplay()", t);
            }
            if (!cameraConfigured) {
                Camera.Parameters parameters = mCamera.getParameters();
//                Camera.Size size = getBestPreviewSize(640 , 480, parameters);
//                Camera.Size size = getBestPreviewSize(800 , 600, parameters);
                Camera.Size size = getBestPreviewSize(640, 480, parameters);
                Camera.Size pictureSize = getBiggestPictureSize(parameters);

                if (size != null && pictureSize != null) {
                    parameters.setPreviewSize(pictureSize.width, pictureSize.height);
                    parameters.setPictureSize(pictureSize.width, pictureSize.height);
                    parameters.setPictureFormat(ImageFormat.JPEG);
                    mCamera.setParameters(parameters);
                    cameraConfigured = true;
                }
            }
        }
    }

    private void startPreview() {
        if (cameraConfigured && mCamera != null) {
            mCamera.startPreview();
            mCamera.setErrorCallback(new Camera.ErrorCallback() {
                @Override
                public void onError(int error, Camera camera) {
                    cameraError = true;
                    Toast.makeText(getContext(), "Camera.setErrorCallback==error=" + error, Toast.LENGTH_SHORT).show();
                }
            });
            if (surfaceCreatedListener != null) {
                surfaceCreatedListener.surfaceCreated();
            }
        }
    }


    private Camera.Size getBestPreviewSize(int width, int height, Camera.Parameters parameters) {
        Camera.Size result = null;
        for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
            if (size.width <= width && size.height <= height) {
                if (result == null) {
                    result = size;
                } else {
                    int resultArea = result.width * result.height;
                    int newArea = size.width * size.height;
                    if (newArea > resultArea) {
                        result = size;
                    }
                }
            }
        }
        return (result);
    }

    private Camera.Size getBiggestPictureSize(Camera.Parameters parameters) {
        Camera.Size result = null;
        for (Camera.Size size : parameters.getSupportedPictureSizes()) {
            if (result == null) {
                result = size;
            } else {
                int resultArea = result.width * result.height;
                int newArea = size.width * size.height;
                if (newArea > resultArea) {
                    result = size;
                }
            }
        }
        return (result);
    }

    private Camera.Size getSmallestPictureSize(Camera.Parameters parameters) {
        Camera.Size result = null;
        for (Camera.Size size : parameters.getSupportedPictureSizes()) {
            if (result == null) {
                result = size;
            } else {
                int resultArea = result.width * result.height;
                int newArea = size.width * size.height;
                if (newArea < resultArea) {
                    result = size;
                }
            }
        }
        return (result);
    }
}
