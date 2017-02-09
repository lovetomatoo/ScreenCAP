package com.quanmin.guohongxin.screencap;

import android.annotation.TargetApi;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;

import java.io.IOException;
import java.nio.ByteBuffer;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private String TAG = getClass().getSimpleName();

    private Button mBtnCap;
    private MediaProjectionManager mMediaProjectionManager;
    private Intent mScreenCaptureIntent;
    private int REQUEST_CODE = 100;
    private VirtualDisplay mVirtualDisplay;
    private SurfaceView mSvMain;
    private int mDensityDpi;

    private static final String MIME_TYPE = "video/avc"; // H.264 Advanced Video Coding
    private MediaCodec mCodec;
    private Surface mInputSurface;

    private MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

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
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
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

        try {
            prepareEncoder();
        } catch (IOException e) {
            e.printStackTrace();
        }

//        Surface surface = creatSurface();

        mVirtualDisplay = mediaProjection.createVirtualDisplay(TAG,
                mSvMain.getWidth(),
                mSvMain.getHeight(),
                mDensityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mInputSurface, null, null);

    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private Surface creatSurface() {

        Surface surface = mSvMain.getHolder().getSurface();

        return surface;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private void prepareEncoder() throws IOException {

        MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, 500, 500);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, 6000000);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 10);

        Log.d(TAG, "created video format: " + format);
        mCodec = MediaCodec.createEncoderByType(MIME_TYPE);
        mCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mInputSurface = mCodec.createInputSurface();
        Log.d(TAG, "created input surface: " + mInputSurface);
        mCodec.start();

        //
        new Thread(new Runnable() {
            @Override
            public void run() {
                resetOutputFormat();
            }
        }).start();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private void resetOutputFormat() {

        MediaFormat outputFormat = mCodec.getOutputFormat(); // 方式二
        for (; ; ) {
            int outputBufferId = mCodec.dequeueOutputBuffer(mBufferInfo, 10000);
            if (outputBufferId >= 0) {
                ByteBuffer outputBuffer = mCodec.getOutputBuffer(outputBufferId);
                // 方式一
                MediaFormat bufferFormat = mCodec.getOutputFormat(outputBufferId);
                mCodec.releaseOutputBuffer(outputBufferId, false);

            } else if (outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                outputFormat = mCodec.getOutputFormat(); // 方式二
            }
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mCodec.stop();
        mCodec.release();
    }
}
