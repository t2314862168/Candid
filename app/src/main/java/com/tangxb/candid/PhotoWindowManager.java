package com.tangxb.candid;

import android.content.Context;
import android.graphics.PixelFormat;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.FrameLayout;

public class PhotoWindowManager {
    /**
     * 屏幕宽度
     */
    private int screenWidth;
    /**
     * 屏幕高度
     */
    private int screenHeight;
    /**
     * 小悬浮窗View的布局
     */
    private FrameLayout frameLayout;
    private WindowManager mWindowManager;
    private Context mContext;
    /**
     * 包裹SurfaceView的容器
     */
    private FrameLayout mSurfaceContainer;
    private TakePictureManger itt;

    /**
     * 创建一个小悬浮窗。初始位置为屏幕的右部中间位置。
     *
     * @param context 必须为应用程序的Context.
     */
    public void createSmallWindow(Context context) {
        mContext = context;
        WindowManager windowManager = getWindowManager(context);
        // 这里多加了一层布局,可以从布局里面获取宽高不需要写固定的
        frameLayout = new FrameLayout(context);
        LayoutInflater.from(context).inflate(R.layout.float_window_small, frameLayout);
        View containerView = frameLayout.findViewById(R.id.small_window_layout);
        int viewWidth = containerView.getLayoutParams().width;
        int viewHeight = containerView.getLayoutParams().height;
        WindowManager.LayoutParams layoutParams = new LayoutParams();
        layoutParams.type = LayoutParams.TYPE_PHONE;
        layoutParams.format = PixelFormat.RGBA_8888;
        layoutParams.flags = LayoutParams.FLAG_NOT_TOUCH_MODAL | LayoutParams.FLAG_NOT_FOCUSABLE;
        layoutParams.gravity = Gravity.LEFT | Gravity.TOP;

        layoutParams.width = viewWidth;
        layoutParams.height = viewHeight;
//                layoutParams.x = screenWidth;
        layoutParams.y = screenHeight / 2;
        windowManager.addView(frameLayout, layoutParams);
        mSurfaceContainer = (FrameLayout) frameLayout.findViewById(R.id.percent);
    }

    /**
     * 将小悬浮窗从屏幕上移除。
     *
     * @param context 必须为应用程序的Context.
     */
    public void removeSmallWindow(Context context) {
        if (frameLayout != null) {
            WindowManager windowManager = getWindowManager(context);
            windowManager.removeView(frameLayout);
            frameLayout = null;
        }
    }

    /**
     * 如果WindowManager还未创建，则创建一个新的WindowManager返回。否则返回当前已创建的WindowManager。
     *
     * @param context 必须为应用程序的Context.
     * @return WindowManager的实例，用于控制在屏幕上添加或移除悬浮窗。
     */
    private WindowManager getWindowManager(Context context) {
        if (mWindowManager == null) {
            mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            Display display = mWindowManager.getDefaultDisplay();
            DisplayMetrics metrics = new DisplayMetrics();
            display.getMetrics(metrics);
            screenWidth = metrics.widthPixels;
            screenHeight = metrics.heightPixels;
        }
        return mWindowManager;
    }

    public FrameLayout getSurfaceContainer() {
        return mSurfaceContainer;
    }
}
