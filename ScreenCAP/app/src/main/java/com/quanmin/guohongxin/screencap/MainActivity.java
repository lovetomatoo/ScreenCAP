package com.quanmin.guohongxin.screencap;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;

import java.nio.ByteBuffer;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private String TAG = getClass().getSimpleName();

    private Button mBtnCap;
    private MediaProjectionManager mMediaProjectionManager;
    private Intent mScreenCaptureIntent;
    private int REQUEST_CODE = 100;
    private VirtualDisplay mVirtualDisplay;
    private SurfaceView mSvMain;
    private int mDensityDpi;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mDensityDpi = metrics.densityDpi;

        mMediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        mScreenCaptureIntent = mMediaProjectionManager.createScreenCaptureIntent();

        mBtnCap = (Button) findViewById(R.id.btn_cap);
        mBtnCap.setOnClickListener(this);

        mSvMain = (SurfaceView) findViewById(R.id.sv_main);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_cap: {
                startActivityForResult(mScreenCaptureIntent, REQUEST_CODE);
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        MediaProjection mediaProjection = mMediaProjectionManager.getMediaProjection(resultCode, data);
        if (mediaProjection == null) {
            return;
        }

        mVirtualDisplay = mediaProjection.createVirtualDisplay(TAG,
                mSvMain.getWidth(),
                mSvMain.getHeight(),
                mDensityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                creatSurface(), null, null);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private Surface creatSurface() {
//        ImageReader imageReader = ImageReader.newInstance(500, 500, PixelFormat.RGBA_8888, 1);
//        image = imageReader.acquireLatestImage();
//
//        mSurface = imageReader.getSurface();

        Surface surface = mSvMain.getHolder().getSurface();

        return surface;
    }
}
