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
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.nio.ByteBuffer;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private String TAG = getClass().getSimpleName();

    private Button mBtnCap;
    private MediaProjectionManager mMediaProjectionManager ;
    private Intent mScreenCaptureIntent;
    private int REQUEST_CODE = 100;
    private Surface mSurface;
    private VirtualDisplay mVirtualDisplay;
    private ImageView mIvMain;
    private Image image;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mMediaProjectionManager = (MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);
        mScreenCaptureIntent = mMediaProjectionManager.createScreenCaptureIntent();

        mBtnCap = (Button) findViewById(R.id.btn_cap);
        mBtnCap.setOnClickListener(this);

        mIvMain = (ImageView) findViewById(R.id.iv_main);
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
                500,
                500,
                500,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
                creatSurface(), null, null);


        mIvMain.postDelayed(new Runnable() {
            @Override
            public void run() {
                int width = image.getWidth();
                int height = image.getHeight();
                final Image.Plane[] planes = image.getPlanes();
                final ByteBuffer buffer = planes[0].getBuffer();
                //每个像素的间距
                int pixelStride = planes[0].getPixelStride();
                //总的间距
                int rowStride = planes[0].getRowStride();
                int rowPadding = rowStride - pixelStride * width;
                Bitmap bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height,
                        Bitmap.Config.ARGB_8888
                );

                mIvMain.setImageBitmap(bitmap);
            }
        }, 2000);

    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private Surface creatSurface() {
        ImageReader imageReader = ImageReader.newInstance(500, 500, PixelFormat.RGBA_8888, 1);
        image = imageReader.acquireLatestImage();

        mSurface = imageReader.getSurface();
        return mSurface;
    }
}
