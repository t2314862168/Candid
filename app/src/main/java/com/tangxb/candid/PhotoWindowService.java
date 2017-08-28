package com.tangxb.candid;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class PhotoWindowService extends Service {
    private static PhotoWindowService instance;

    public static PhotoWindowService getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (instance == null) {
            synchronized (PhotoWindowService.class) {
                if (instance == null) {
                    instance = this;
                    createSmallWindow();
                }
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        removeSmallWindow();
        instance = null;
    }

    /**
     * 将小悬浮窗添加到屏幕上
     */
    private void createSmallWindow() {
        TakePictureManger.getInstance(getApplicationContext()).createSmallWindow();
    }

    /**
     * 将小悬浮窗从屏幕上移除
     */
    private void removeSmallWindow() {
        TakePictureManger.getInstance(getApplicationContext()).removeSmallWindow();
    }

    /**
     * 拍照
     */
    public void cameraTakePhoto(String betweenStr) {
        TakePictureManger.getInstance(getApplicationContext()).cameraTakePhoto(betweenStr);
    }
}
