package com.quanmin.guohongxin.screencap.pushstream;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.MediaCodec;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Toast;

import com.tv.quanmin.libfilter.CameraSurfaceView;
import com.tv.quanmin.libfilter.QmAudioEncoder;
import com.tv.quanmin.libfilter.QmSurfaceChangedListener;
import com.tv.quanmin.libfilter.QmSurfaceCreatedListener;
import com.tv.quanmin.libfilter.RenderSrfTex;
import com.tv.quanmin.libfilter.RetriveSurfaceDataListener;
import com.tv.quanmin.libfilter.SrsHttpFlv;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import tv.quanmin.livestreamlibrary.RtmpHelperJNI;
import tv.quanmin.livestreamlibrary.StreamStateListener;

/**
 * 功能如下：
 * 设置推流的url 码率
 * 美颜
 * 切换摄像头
 * 打开和关闭闪光灯
 *
 * usage : pushView.setStreamParams(480,640,500,TestActivity.mRtmpUrl);
 *
 * 注意：
 * 1.分辨率无法保证适配 每个手机 因为 每个手机的相机 可使用采集分辨率不同
 * 2.目前必须在页面显示之前 setStreamParams 要修改 需要重新进入页面才可以临时修改码率分辨率等信息
 */
public class QmPushView extends CameraSurfaceView implements QmSurfaceCreatedListener,
        QmSurfaceChangedListener {

    private static final String TAG = QmPushView.class.getSimpleName();
    private static final int MY_PERMISSIONS_REQUEST_CAMERA = 0;
    private static final int MY_PERMISSIONS_REQUEST_RECORD = 1;

    private static final int VIDEO_TRACK = 100;
    private final static int VGOP = 20;

    private Context mContext;

    private Camera.Parameters mParameters;
    private SrsHttpFlv muxer; // 混流器
    private QmAudioEncoder mAEncoder; // 音频编码
    private RenderSrfTex mRenderSrfTex; // 视频编码

    //推流地址。注意：正式使用时应该置为空字符串
    protected String mRtmpUrl = "";

//    protected int mCameraId = -1;

    //默认分辨率
    private int mVwidth = 368;
    private int mVheight = 640;

    private Object lockEncoder = null;
    private Object lockCamera = null;

    private boolean bMirror = true; // 默认开启镜像功能

    // the bitrate in kbps.
    private int vbitrate_kbps = 1000;
    private int VFPS = 24;

    // encoding params.
    private long mSysTimeUs;

    protected RetriveSurfaceDataListener mRetrieveDataListerner = new RetriveSurfaceDataListener() {
        @Override
        public void retrieveData(ByteBuffer byteBuf, MediaCodec.BufferInfo bufferInfo) {
            try {
                if (muxer != null) {
                    muxer.writeSampleData(VIDEO_TRACK, byteBuf, bufferInfo);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    public QmPushView(Context context) {
        super(context);
        mContext = context;
        muxer = null;
        init();
    }

    public QmPushView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        muxer = null;
        init();
    }

    private void init() {
        bMirror = false;
        lockEncoder = new Object();
        lockCamera = new Object();
        setSurfaceCreatedListener(this);
        setSurfaceChangedListener(this);
    }

    /**
     * 需要尽早的设置这些参数 否则以默认参数上传
     *
     * @param w        宽度
     * @param h        高度
     * @param vbitrate 码率
     * @param frame    帧数
     * @param url      推流地址
     */
    public void setStreamParams(int w, int h, int vbitrate, int frame, String url) {
        mVwidth = w;
        mVheight = h;
        mRtmpUrl = url;
        vbitrate_kbps = vbitrate;
        VFPS = frame;

        setPreviewSize(mVwidth, mVheight);
        setSysFps(VFPS);

        Log.d(TAG, "setStreamParams " + mVwidth + "  " + mVheight + " " + mRtmpUrl + " "
                + vbitrate_kbps + " frame = " + frame);
    }

    /**
     * 是否为前置摄像头
     *
     * @return boolean
     */
    public boolean isFrontCamera() {
        return QmPushCamera.getInstance(getContext()).isFrontCamera();
    }

    /**
     * 切换摄像头
     */
    public void switchCamera() {
        QmPushCamera.getInstance(getContext()).switchCamera();
    }

    /**
     * 开启相机预览
     */
    public void startCameraPreview() {
        QmPushCamera.getInstance(getContext()).startPreview();
    }

    /**
     * 关闭相机预览。并没有释放Camera资源
     */
    public void stopCameraPreview() {
        QmPushCamera.getInstance(getContext()).stopPreview();
    }

    /**
     * 是否开启闪光灯
     */
    public boolean isOpenFlashLight() {
        return QmPushCamera.getInstance(getContext()).isOpenFlashLight();
    }

    /**
     * 打开摄像头并开启闪光灯
     */
    public void openFlashLight() {
        QmPushCamera.getInstance(getContext()).openFlashLight();
    }

    /**
     * 关闭闪光灯
     */
    public void closeFlashLight() {
        QmPushCamera.getInstance(getContext()).closeFlashLight();
    }

    /**
     * 开启美颜
     */
    public void openEffect() {
        super.openEffect();
    }

    /**
     * 关闭美颜
     */
    public void closeEffect() {
        super.closeEffect();
    }

    /**
     * 美颜参数设置：锐化
     *
     * @param range 0.0~1.0
     */
    public void setSharpness(final float range) {
//        super.setSharpness(range);
    }

    /**
     * 美颜参数设置：色彩
     *
     * @param range 0.0~1.0
     */
    public void setHue(final float range) {
//        super.setHue(range);
    }

    /**
     * 美颜参数设置：亮度
     *
     * @param range 0.0~1.0
     */
    public void setBrightness(final float range) {
//        super.setBrightness(range);
    }

    /**
     * 美颜参数设置：对比度
     *
     * @param range 0.0~1.0
     */
    public void setContrast(final float range) {
//        super.setContrast(range);
    }

    /**
     * 美颜参数设置：饱和度
     *
     * @param range 0.0~1.0
     */
    public void setSaturation(final float range) {
//        super.setSaturation(range);
    }

    /**
     * 美颜参数设置：曝光
     *
     * @param range 0.0~1.0
     */
    public void setExposure(final float range) {
//        super.setExposure(range);
    }

    public void setBeautyLevel(final float range) {
//        super.setBeautyLevel(range);
    }

    public void setBeautyWithToneLevel(float toneLevel, float beautyLevel, float brightLevel) {
//        super.setBeautyWithToneLevel(toneLevel,beautyLevel,brightLevel);
    }

    /**
     * 镜像开关
     *
     * @param bMirror 默认为true，开启镜像
     */
    public void setMirror(boolean bMirror) {
        Log.d(TAG, "bMirror = " + bMirror);
        if (isFrontCamera()) {
            if (mRenderSrfTex != null) {
                mRenderSrfTex.setMirror(bMirror);
            } else {
                this.bMirror = bMirror;
            }
        } else {
            this.bMirror = false;
        }
    }

    @Override
    public void onResume() {
        if (ContextCompat.checkSelfPermission(getContext(),
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.CAMERA},
                    MY_PERMISSIONS_REQUEST_CAMERA);
        } else {
//            QmPushCamera.getInstance(getContext()).onResume();
        }
        super.onResume();
    }

    private Activity getActivity() {
        return (Activity) getContext();
    }

    @Override
    public void onPause() {
        super.onPause();
        dispose();
    }

    @Override
    public void onSurfaceCreated(SurfaceTexture inputSurfaceTexture, RenderSrfTex srfTex) {

//        mSurfaceTexture = inputSurfaceTexture;
//        inputSurfaceTexture.setOnFrameAvailableListener(new SurfaceTexture
// .OnFrameAvailableListener() {
//            @Override
//            public void onFrameAvailable(SurfaceTexture surfaceTexture) {
//            showPreview();
//            }
//        });

        if (ContextCompat.checkSelfPermission(getContext(),
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.CAMERA},
                    MY_PERMISSIONS_REQUEST_CAMERA);
        } else {
            QmPushCamera.getInstance(getContext()).onResume();
        }

        QmPushCamera.getInstance(getContext()).startCameraPreview(inputSurfaceTexture);

        try {
            initEncoder(mVwidth, mVheight);
        } catch (IOException e) {
            e.printStackTrace();
            destroyEncoder();
            return;
        }

        if (mRenderSrfTex == null) {
            mRenderSrfTex = new RenderSrfTex(
                    mVwidth, mVheight,
                    mParameters,
                    mRetrieveDataListerner,
                    mSysTimeUs,
                    vbitrate_kbps, VFPS);
            setRenderSrfTex(mRenderSrfTex);
        }

        // 由于视频编码器有可能尚未创建好，导致setMirror没能生效。此时再次执行一遍
        if (isFrontCamera()) {
            setMirror(bMirror);
        } else {
            setMirror(false);
        }

        // 检查摄像头配置是否正确
//        sendBroadcastFilter(BroadCofig.BROAD_PUSH_MIRROR_CHECK);
    }

    @Override
    public void onSurfaceChanged(SurfaceTexture inputSurfaceTexture, RenderSrfTex srfTex) {

    }

    @SuppressLint("NewApi")
    public void initEncoder(int width, int height)
            throws IOException {
        if (TextUtils.isEmpty(mRtmpUrl)) {
            return;
        }

        if (muxer != null) {
            return;
        }

        if (ContextCompat.checkSelfPermission(getContext(),
                Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    MY_PERMISSIONS_REQUEST_RECORD);
            return;
        }

        muxer = new SrsHttpFlv(mContext, mRtmpUrl, 0,
                android.os.Build.MODEL,
                android.os.Build.VERSION.RELEASE,
                vbitrate_kbps,
                VFPS);

        muxer.setStreamStateListener(new StreamStateListener() {
            @Override
            public void onReonnectSecond(long second) {
                if (this.getNoticeArr().indexOfKey((int) second) > -1) {
                    toast(this.getNoticeArr().get((int) second));

                    if ((int) second == StreamStateListener.STOP_SECOND) {
                        getActivity().finish();
                    }
                }
            }
        });

        try {
            muxer.start();
        } catch (IOException e) {
            Log.e(TAG, "start muxer failed.");
            throw e;
        }
        Log.i(TAG, String.format("start muxer to SRS over HTTP FLV, url=%s", mRtmpUrl));

        mSysTimeUs = new Date().getTime() * 1000;

        mAEncoder = new QmAudioEncoder(mContext, muxer, mSysTimeUs);
        mAEncoder.initAudioEncoder();
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void destroyEncoder() {
        synchronized (lockEncoder) {
            if (mAEncoder != null) {
                mAEncoder.destroy();
                mAEncoder = null;
            }

            RtmpHelperJNI.spx_denoise_destroy();

            setRenderSrfTex(null);
            if (mRenderSrfTex != null) {
                mRenderSrfTex.stop();
                mRenderSrfTex = null;
            }

            //RtmpHelperJNI.close();
            if (muxer != null) {
                Log.i(TAG, "stop muxer to SRS over HTTP FLV");
                muxer.stop();
                muxer.release();
                muxer = null;
            }
        }
    }

    @SuppressLint("NewApi")
    protected void dispose() {
        destroyEncoder();
        QmPushCamera.getInstance(getContext()).quitCamera();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        dispose();
    }

//    private float oldDist = 1f;

    /**
     * 双指拖动缩放  和 单指点击对焦
     */
    public boolean onTouchEvent(MotionEvent event) {
        synchronized (lockCamera) {
            if (event.getPointerCount() == 1) {
//                handleFocusMetering(event, mCamera);
            } else {
                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_POINTER_DOWN:
//                        oldDist = getFingerSpacing(event);
                        break;
                    case MotionEvent.ACTION_MOVE:
//                        float newDist = getFingerSpacing(event);
//                        if (newDist > oldDist) {
//                            handleZoom(true, mCamera);
//                        } else if (newDist < oldDist) {
//                            handleZoom(false, mCamera);
//                        }
//                        oldDist = newDist;
                        break;
                }
            }
        }
        return true;
    }


    private void handleZoom(boolean isZoomIn, Camera camera) {
        if (camera == null) {
            return;
        }
        Camera.Parameters params = camera.getParameters();
        if (params.isZoomSupported()) {
            int maxZoom = params.getMaxZoom();
            int zoom = params.getZoom();
            if (isZoomIn && zoom < maxZoom) {
                zoom++;
            } else if (zoom > 0) {
                zoom--;
            }
            params.setZoom(zoom);
            camera.setParameters(params);
        } else {
            Log.i(TAG, "zoom not supported");
        }
    }

    private static void handleFocusMetering(MotionEvent event, Camera camera) {

        if (camera == null) {
            return;
        }

        if (camera.getParameters().getSupportedFocusModes().contains(
                Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {

        } else if (camera.getParameters().getSupportedFocusModes().contains(
                Camera.Parameters.FOCUS_MODE_MACRO)) {//is support auto focus,dont hand focus

            Camera.Parameters params = camera.getParameters();
            Camera.Size previewSize = params.getPreviewSize();
            Rect focusRect = calculateTapArea(event.getX(), event.getY(), 1f, previewSize);
            Rect meteringRect = calculateTapArea(event.getX(), event.getY(), 1.5f, previewSize);

            camera.cancelAutoFocus();

            if (params.getMaxNumFocusAreas() > 0) {
                List<Camera.Area> focusAreas = new ArrayList<>();
                focusAreas.add(new Camera.Area(focusRect, 800));
                params.setFocusAreas(focusAreas);
            } else {
                Log.i(TAG, "focus areas not supported");
            }
            if (params.getMaxNumMeteringAreas() > 0) {
                List<Camera.Area> meteringAreas = new ArrayList<>();
                meteringAreas.add(new Camera.Area(meteringRect, 800));
                params.setMeteringAreas(meteringAreas);
            } else {
                Log.i(TAG, "metering areas not supported");
            }
            try {
                final String currentFocusMode = params.getFocusMode();
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_MACRO);
                camera.setParameters(params);

                camera.autoFocus(new Camera.AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(boolean success, Camera camera) {

                        Camera.Parameters params = camera.getParameters();
                        params.setFocusMode(currentFocusMode);
                        camera.setParameters(params);
                    }
                });
            } catch (Exception ex) {
            }
        }
    }


    private static Rect calculateTapArea(float x, float y, float coefficient,
                                         Camera.Size previewSize) {
        float focusAreaSize = 300;
        int areaSize = Float.valueOf(focusAreaSize * coefficient).intValue();
        int centerX = (int) (x / previewSize.width - 1000);
        int centerY = (int) (y / previewSize.height - 1000);

        int left = clamp(centerX - areaSize / 2, -1000, 1000);
        int top = clamp(centerY - areaSize / 2, -1000, 1000);

        RectF rectF = new RectF(left, top, left + areaSize, top + areaSize);

        return new Rect(Math.round(rectF.left), Math.round(rectF.top), Math.round(rectF.right),
                Math.round(rectF.bottom));
    }


    private static int clamp(int x, int min, int max) {
        if (x > max) {
            return max;
        }
        if (x < min) {
            return min;
        }
        return x;
    }

    private static float getFingerSpacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    /**
     * 是否连接
     */
    public boolean isConnect() {
        return RtmpHelperJNI.isConnected() == 1;
    }

    /**
     * 得到流量
     */
    public int getBytes() {
        return RtmpHelperJNI.getKps();
    }

    //丢包率 0-100
    public int getLossFrame() {
        return 0;
    }

    private void toast(final String errorMsg) {
        if (TextUtils.isEmpty(errorMsg)) {
            return;
        }

        post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getContext(), errorMsg, Toast.LENGTH_SHORT).show();
            }
        });

    }

    public boolean isHorScreen() {
        Configuration mConfiguration = getContext().getResources().getConfiguration(); //获取设置的配置信息
        int ori = mConfiguration.orientation; //获取屏幕方向
        if (ori == Configuration.ORIENTATION_LANDSCAPE) {
            return true;
        } else if (ori == Configuration.ORIENTATION_PORTRAIT) {
            return false;
        }
        return false;
    }

    private void sendBroadcastFilter(String filter) {
        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(new Intent(filter));
    }
}
