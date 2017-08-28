package com.tangxb.candid;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PictureCallback;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * 设置定时拍照功能
 * http://blog.csdn.net/pbm863521/article/details/76429132
 * https://stackoverflow.com/questions/15130447/android-camera-setdisplayorientation-does-not-work
 */
public class TakePictureManger {
    private Object object = new Object();
    private static TakePictureManger mInstance;
    private static String TAG = TakePictureManger.class.getSimpleName();
    /**
     * 对焦时间
     */
    private final long autoFocusTime = 1000L;
    private Context mContext;
    private CameraPreview mPreview;
    private String saveLocation;
    private final String extension = "jpg";
    private final int cameraStop = Integer.MAX_VALUE;
    private ExecutorService mExecutorService;
    private ExecutorService mExecutorService2;
    private ExecutorService mExecutorService3;
    private volatile int number = 0;
    private File dirF;
    private Bitmap originBitmap;
    private int rotateAngle = 90;
    private Matrix matrix = new Matrix();
    private ConcurrentLinkedQueue<String> mQueue;
    private PhotoWindowManager myWindowManager;
    /**
     * 是否能够继续拍照
     */
    private boolean canFlag = true;

    public static TakePictureManger getInstance(Context context) {
        if (mInstance == null) {
            synchronized (TakePictureManger.class) {
                if (mInstance == null) {
                    mInstance = new TakePictureManger(context);
                }
            }
        }
        return mInstance;
    }

    private TakePictureManger(Context context) {
        this.mContext = context;
        init();
    }

    public void init() {
        myWindowManager = new PhotoWindowManager();
        mExecutorService3 = Executors.newSingleThreadExecutor();
        mExecutorService = Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable);
                thread.setName("Thread--" + number);
                return thread;
            }
        });
        mExecutorService2 = Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable);
                thread.setName("PictureCallbackThread--");
                return thread;
            }
        });
//        saveLocation = Environment.getExternalStorageDirectory().getAbsolutePath() + "/aTestDir";
//        saveLocation = context.getFilesDir().getAbsolutePath() + "/aTestDir";
        saveLocation = "/mnt/sdcard/aTestDir";
        // 获取Jpeg图片，并保存在sd卡上
        dirF = new File(saveLocation);
        if (!dirF.exists()) {
            dirF.mkdirs();
        }
        mQueue = new ConcurrentLinkedQueue<>();
    }

    /**
     * 将小悬浮窗添加到屏幕上
     */
    public void createSmallWindow() {
        myWindowManager.createSmallWindow(mContext);
        initView(myWindowManager.getSurfaceContainer());
    }

    /**
     * 将小悬浮窗从屏幕上移除
     */
    public void removeSmallWindow() {
        myWindowManager.removeSmallWindow(mContext);
        stop();
    }

    public void initView(FrameLayout surfaceContainer) {
        mPreview = new CameraPreview(mContext);
        surfaceContainer.addView(mPreview);
    }

    /**
     * 这里需要加上睡眠时间
     */
    public void cameraTakePhoto(final String betweenStr) {
        if (mPreview.isCameraError()) {
            Toast.makeText(mContext, "cameraError==", Toast.LENGTH_SHORT).show();
            return;
        }
        mExecutorService3.execute(new Runnable() {
            @Override
            public void run() {
                synchronized (object) {
                    while (!canFlag) {
                        try {
                            object.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    Log.v(TAG, "cameraTakePhoto==" + Thread.currentThread().getName());
                    mQueue.add(betweenStr);
                    mExecutorService.execute(new PhotoRunnable(autoFocusTime));
                    canFlag = false;
                }
            }
        });
    }

    /**
     * takePhoto()里面的对焦是异步的需要时间;如果把sleepTime设置为0则会出问题
     */
    private void takePhoto() {
        synchronized (object) {
            Log.v(TAG, "takePhoto==" + Thread.currentThread().getName());
            if (mPreview.getCamera() == null) {
                resetData();
                number = 0;
                canFlag = true;
                object.notifyAll();
            } else {
                if (number < cameraStop) {
                    mPreview.getCamera().autoFocus(new AutoFocusCallback() {
                        @Override
                        public void onAutoFocus(boolean success, Camera camera) {
                            Log.v(TAG, "onAutoFocus====success==" + success);
                            Log.v(TAG, "onPictureTaken==" + Thread.currentThread().getName());
                            // 从Camera捕获图片
                            try {
                                mPreview.getCamera().takePicture(null, null, mPicture);
                            } catch (Exception e) {
                                canFlag = false;
                                object.notifyAll();
                                Log.v(TAG, "mCamera.takePicture==Exception=" + e.getMessage());
                                resetData();
                            }
                        }
                    });
                } else {
                    canFlag = true;
                    object.notifyAll();
                    Log.v(TAG, "Camera.autoFocus====releaseCamera");
                    resetData();
                }
            }
        }
    }


    /**
     * 输出的照片为最高像素
     */
    public void setPictureSize(Camera.Parameters parametes) {
        List<Camera.Size> localSizes = parametes.getSupportedPictureSizes();
        Camera.Size biggestSize = null;
        Camera.Size fitSize = null;// 优先选预览界面的尺寸
        Camera.Size previewSize = parametes.getPreviewSize();
        float previewSizeScale = 0;
        if (previewSize != null) {
            previewSizeScale = previewSize.width / (float) previewSize.height;
        }

        if (localSizes != null) {
            int cameraSizeLength = localSizes.size();
            for (int n = 0; n < cameraSizeLength; n++) {
                Camera.Size size = localSizes.get(n);
                if (biggestSize == null) {
                    biggestSize = size;
                } else if (size.width >= biggestSize.width && size.height >= biggestSize.height) {
                    biggestSize = size;
                }

                // 选出与预览界面等比的最高分辨率
                if (previewSizeScale > 0
                        && size.width >= previewSize.width && size.height >= previewSize.height) {
                    float sizeScale = size.width / (float) size.height;
                    if (sizeScale == previewSizeScale) {
                        if (fitSize == null) {
                            fitSize = size;
                        } else if (size.width >= fitSize.width && size.height >= fitSize.height) {
                            fitSize = size;
                        }
                    }
                }
            }

            // 如果没有选出fitSize, 那么最大的Size就是FitSize
            if (fitSize == null) {
                fitSize = biggestSize;
            }
            parametes.setPictureSize(fitSize.width, fitSize.height);
        }
    }

    private PictureCallback mPicture = new PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            mExecutorService2.execute(new PhotoCallBackRunnable(data));
        }
    };

    public Bitmap rotateBitmap(int angle, Bitmap bitmap) {
        matrix.reset();
        //旋转图片 动作
        matrix.postRotate(angle, (float) bitmap.getWidth() / 2, (float) bitmap.getHeight() / 2);
        // 创建新的图片
        Bitmap resizedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        return resizedBitmap;
    }

    public void resetData() {
        number = 0;
        mQueue.clear();
    }

    public void stop() {
        resetData();
        mExecutorService.shutdown();
        mExecutorService2.shutdown();
        mExecutorService3.shutdown();
        mInstance = null;
    }

    /**
     * 保存图片处理
     */
    class PhotoCallBackRunnable implements Runnable {
        private byte[] data;

        public PhotoCallBackRunnable(byte[] data) {
            this.data = data;
        }

        @Override
        public void run() {
            synchronized (object) {
                if (mPreview.getCamera() != null) {
                    mPreview.getCamera().startPreview();
                }
                String betweenStr = null;
                String photoFilePath = null;
                boolean result = false;
                try {
                    File pictureFile = new File(saveLocation + "/" + System.currentTimeMillis() + "." + extension);//扩展名
                    originBitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                    //前置摄像头需要旋转270度，后置摄像头需要旋转90度
                    Bitmap rotateBitmap = rotateBitmap(rotateAngle, originBitmap);
                    BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(pictureFile));
                    originBitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);//将图片压缩到流中
                    bos.flush();
                    bos.close();
                    if (!rotateBitmap.isRecycled()) {
                        rotateBitmap.recycle();
                    }
                    if (!originBitmap.isRecycled()) {
                        originBitmap.recycle();
                    }
                    Log.v(TAG, "图片第==" + (number + 1) + "==张保存图成功");
                    Log.v(TAG, "onPictureTaken==" + Thread.currentThread().getName());
                    number++;
                    photoFilePath = pictureFile.getAbsolutePath();
                    result = true;
                } catch (Exception e) {
                    Log.v(TAG, "图片第==" + (number + 1) + "==保存图片失败");
                    e.printStackTrace();
                } finally {
                    canFlag = true;
                    object.notifyAll();
                    betweenStr = mQueue.poll();
                    Log.v(TAG, "betweenStr==" + betweenStr + "photoFilePath==" + photoFilePath);
                }
            }
        }
    }

    /**
     * 这里需要睡眠时间,因为takePhoto()里面的对焦是异步的需要时间;如果把sleepTime设置为0则会出问题
     */
    class PhotoRunnable implements Runnable {
        private long sleepTime;

        public PhotoRunnable(long sleepTime) {
            this.sleepTime = sleepTime;
        }

        @Override
        public void run() {
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (mPreview.isSurfaceCreated()) {
                takePhoto();
            } else {
                mPreview.setSurfaceCreatedListener(new CameraPreview.SurfaceCreatedListener() {
                    @Override
                    public void surfaceCreated() {
                        takePhoto();
                    }
                });
            }
        }
    }
}
