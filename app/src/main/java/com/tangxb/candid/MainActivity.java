package com.tangxb.candid;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {
    private Button startVideo;
    private Button stopVideo;
    private Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        intent = new Intent(MainActivity.this, PhotoWindowService.class);
        startService(intent);
        startVideo = (Button) findViewById(R.id.btn_start_video);
        stopVideo = (Button) findViewById(R.id.btn_stop_video);
        startVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (intent == null) {
                    intent = new Intent(MainActivity.this, PhotoWindowService.class);
                    startService(intent);
                }
                test_1();
            }
        });
        stopVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //解绑服务
                if (intent != null) {
                    stopService(intent);
                    intent = null;
                }
            }
        });
    }

    int peopleIndex;
    int productCount = 1;

    /**
     * 顾客index+商品在购物车的index
     */
    public void test_1() {
        if (PhotoWindowService.getInstance() == null) return;
        for (int i = 0; i < productCount; i++) {
            PhotoWindowService.getInstance().cameraTakePhoto(peopleIndex + "-" + i);
        }
        peopleIndex++;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (intent != null) {
            stopService(intent);
        }
    }
}
