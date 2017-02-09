package com.quanmin.guohongxin.screencap.pushstream;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.Log;

import com.tv.quanmin.libfilter.CameraUtil;
import com.tv.quanmin.libfilter.VideoParam;

import java.io.IOException;
import java.util.List;

/**
 * Created by xiaoxi on 9/6/16.
 */
public class QmPushCamera {

    protected Camera mCamera;
    protected int mCameraId = -1;
    public static QmPushCamera mCameraInstance;
    private final static String TAG = QmPushCamera.class.getSimpleName();
    private Context mContext;
    private boolean isOpenLight = false;
    private Object lockCamera = null;
    SurfaceTexture mSurfaceTexture;

    public static synchronized QmPushCamera getInstance(Context context) {
        if (mCameraInstance == null) {
            mCameraInstance = new QmPushCamera(context);
        }
        return mCameraInstance;
    }

    public QmPushCamera(Context context) {
        mContext = context;
        lockCamera = new Object();
    }

    public void startPreview() {
        if (mCamera != null) {
            mCamera.startPreview();
        }
    }

    public void stopPreview() {
        if (mCamera != null) {
            mCamera.stopPreview();
        }
    }

    public void onResume() {
        if (mCameraId < 0) {
            initCamera();
        } else {
            reInitCamera(mCameraId);
        }
    }

    public void quitCamera() {
        synchronized (lockCamera) {
            if (mCamera != null) {
                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
            }
        }
    }

    public boolean isOpenFlashLight() {
        String flashModel = mCamera.getParameters().getFlashMode() + ""; // 防止flashModel == null
        return !isFrontCamera() && (!Camera.Parameters.FLASH_MODE_OFF.equals(flashModel));
    }

    public void openFlashLight() {
        isOpenLight = true;
        if (isFrontCamera()) {
            return;
        }
        synchronized (lockCamera) {
            if (mCamera == null) {
                return;
            }
            Camera.Parameters p = mCamera.getParameters();
            String flashMode = p.getFlashMode();
            if (!Camera.Parameters.FLASH_MODE_TORCH.equals(flashMode)) {
                mCamera.stopPreview();
                p.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                mCamera.setParameters(p);
                mCamera.startPreview();
            }
        }
    }

    public void closeFlashLight() {
        isOpenLight = false;
        synchronized (lockCamera) {
            if (mCamera == null) {
                return;
            }
            Camera.Parameters p = mCamera.getParameters();
            String flashMode = p.getFlashMode();

            if (!Camera.Parameters.FLASH_MODE_OFF.equals(flashMode)) {
                mCamera.stopPreview();
                p.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                mCamera.setParameters(p);
                mCamera.startPreview();
            }
        }
    }

    public boolean isFrontCamera() {
        return mCameraId == CameraUtil.CAMERA_TYPE_FRONT;
    }

    public void switchCamera() {
        int next_camera = CameraUtil.getNextCamera(getCurrentId());
        if (next_camera >= 0 && next_camera != getCurrentId())//如果能获取得到
        {
            quitCamera();
            restartCamera(next_camera);
        }
    }

    public void startCameraPreview(SurfaceTexture surface) {
        mSurfaceTexture = surface;
        synchronized (lockCamera) {
            if (mCamera == null) {
                return;
            }
            try {
                mCamera.setPreviewTexture(surface);
            } catch (IOException t) {
            }
            mCamera.startPreview();
        }
    }

    private Activity getActivity() {
        return (Activity) mContext;
    }

    public void openCamera() {
        if (mCamera != null) {
            return;
        }
        mCameraId = CameraUtil.findFrontCamera();
        if (mCameraId == -1) {
            mCameraId = CameraUtil.findBackCamera();
        }
        mCamera = Camera.open(mCameraId);
    }

    public void initCameraExt() {
        if (!isHorScreen()) {
            mCamera.setDisplayOrientation(90);
        }
        Camera.Parameters mParams = mCamera.getParameters();
        VideoParam mVideoParam = new VideoParam();
        mVideoParam.init(mParams);
        List<Camera.Size> sizes = mParams.getSupportedPreviewSizes();
        Camera.Size size = CameraUtil.getOptimalPreviewSize(getActivity(), sizes);
        mParams.setPreviewSize(size.width, size.height);

        List<String> focusModes = mParams.getSupportedFocusModes();
        if (focusModes.contains("continuous-video")) {
            mParams.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        }
        mCamera.setParameters(mParams);

        mCamera.startPreview();
    }

    private void initCamera() {
//        if (!isHorScreen()) {
//            mCamera.setDisplayOrientation(90);
//        }
//        Camera.Parameters mParams = mCamera.getParameters();
//        VideoParam mVideoParam = new VideoParam();
//        mVideoParam.init(mParams);
//        List<Camera.Size> sizes = mParams.getSupportedPreviewSizes();
//        Camera.Size size = CameraUtil.getOptimalPreviewSize(getActivity(), sizes);
//        mParams.setPreviewSize(size.width, size.height);
//
//        List<String> focusModes = mParams.getSupportedFocusModes();
//        if(focusModes.contains("continuous-video")){
//            mParams.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
//        }
//        mCamera.setParameters(mParams);
//
//        mCamera.startPreview();
        try {
            synchronized (lockCamera) {
                if (mCamera != null) {
                    return;
                }
                mCameraId = CameraUtil.findFrontCamera();
                Log.e(TAG, "initCamera [CAMERA ID] front = " + mCameraId);
                if (mCameraId == -1) {
                    mCameraId = CameraUtil.findBackCamera();
                    Log.e(TAG, "[CAMERA ID] back = " + mCameraId);
                }
                mCamera = Camera.open(mCameraId);

                if (!isHorScreen()) {
                    mCamera.setDisplayOrientation(90);
                }
                Camera.Parameters cp = mCamera.getParameters();
                if (cp.getSupportedFocusModes().contains(
                        Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                    cp.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                }
                VideoParam mVideoParam = new VideoParam();
                mVideoParam.init(cp);
//                Camera.Size size = CameraUtil.getQmOptimalPreviewSize(cp,mVwidth,mVheight);
                List<Camera.Size> sizes = cp.getSupportedPreviewSizes();
                Camera.Size size = CameraUtil.getOptimalPreviewSize(getActivity(), sizes);
                Log.i("CameraUtil",
                        String.format("camera select size %dx%d", size.width, size.height));
                cp.setPreviewSize(size.width, size.height);
                if (isOpenLight) {
                    cp.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                }
//                cp.setPreviewFpsRange(
//                        mVideoParam.mFpsRange[Camera.Parameters.PREVIEW_FPS_MIN_INDEX],
//                        mVideoParam.mFpsRange[Camera.Parameters.PREVIEW_FPS_MAX_INDEX]);
//                cameraInited(size.width, size.height);
                mCamera.setParameters(cp);
//                mParameters = cp;
            }
        } catch (Exception e) {
            e.printStackTrace();
            quitCamera();
//            toast("打开相机失败，有可能没有相关权限，请在\"设置\"中打开相机权限");
            Log.e(TAG, "[CAMERA ID] open = " + e.toString());
        }
    }

    private void reInitCamera(int cameraId) {
        try {
            synchronized (lockCamera) {
                if (mCamera != null) {
                    return;
                }

                mCameraId = cameraId;
                Log.e(TAG, "[CAMERA ID] front = " + cameraId);
                if (cameraId == -1) {
                    cameraId = CameraUtil.findFrontCamera();
                    Log.e(TAG, "[CAMERA ID] back = " + cameraId);
                }
                mCamera = Camera.open(cameraId);
                if (!isHorScreen()) {
                    mCamera.setDisplayOrientation(90);
                }
                Camera.Parameters cp = mCamera.getParameters();
                if (cp.getSupportedFocusModes().contains(
                        Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                    cp.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                }
                VideoParam mVideoParam = new VideoParam();
                mVideoParam.init(cp);
//                List<Camera.Size> sizes = cp.getSupportedPreviewSizes();
//                Camera.Size size = CameraUtil.getQmOptimalPreviewSize(cp,mVwidth,mVheight);
                List<Camera.Size> sizes = cp.getSupportedPreviewSizes();
                Camera.Size size = CameraUtil.getOptimalPreviewSize(getActivity(), sizes);
                cp.setPreviewSize(size.width, size.height);
                if (isOpenLight) {
                    cp.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                }
                cp.setPreviewFpsRange(
                        mVideoParam.mFpsRange[Camera.Parameters.PREVIEW_FPS_MIN_INDEX],
                        mVideoParam.mFpsRange[Camera.Parameters.PREVIEW_FPS_MAX_INDEX]);
//                cameraInited(size.width, size.height);
                mCamera.setParameters(cp);
//                mParameters = cp;

//                try {
//                    mCamera.setPreviewTexture(mSurfaceTexture);
//                } catch (IOException t) {
//                }
//                mCamera.startPreview();
            }
        } catch (Exception e) {
            e.printStackTrace();
            quitCamera();
//            toast("打开相机失败，有可能没有相关权限，请在\"设置\"中打开相机权限");
            Log.e(TAG, "[CAMERA ID] open = " + e.toString());
//                throw new RuntimeException("setup camera");
        }
    }

    private void restartCamera(int cameraId) {
        try {
            synchronized (lockCamera) {
                if (mCamera != null) {
                    return;
                }

                mCameraId = cameraId;
                Log.e(TAG, "[CAMERA ID] front = " + cameraId);
                if (cameraId == -1) {
                    cameraId = CameraUtil.findFrontCamera();
                    Log.e(TAG, "[CAMERA ID] back = " + cameraId);
                }
                mCamera = Camera.open(cameraId);
                if (!isHorScreen()) {
                    mCamera.setDisplayOrientation(90);
                }
                Camera.Parameters cp = mCamera.getParameters();
                if (cp.getSupportedFocusModes().contains(
                        Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                    cp.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                }
                VideoParam mVideoParam = new VideoParam();
                mVideoParam.init(cp);
//                List<Camera.Size> sizes = cp.getSupportedPreviewSizes();
//                Camera.Size size = CameraUtil.getQmOptimalPreviewSize(cp,mVwidth,mVheight);
                List<Camera.Size> sizes = cp.getSupportedPreviewSizes();
                Camera.Size size = CameraUtil.getOptimalPreviewSize(getActivity(), sizes);
                cp.setPreviewSize(size.width, size.height);
                if (isOpenLight) {
                    cp.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                }
                cp.setPreviewFpsRange(
                        mVideoParam.mFpsRange[Camera.Parameters.PREVIEW_FPS_MIN_INDEX],
                        mVideoParam.mFpsRange[Camera.Parameters.PREVIEW_FPS_MAX_INDEX]);
//                cameraInited(size.width, size.height);
                mCamera.setParameters(cp);
//                mParameters = cp;

                try {
                    mCamera.setPreviewTexture(mSurfaceTexture);
                } catch (IOException t) {
                }
                mCamera.startPreview();
            }
        } catch (Exception e) {
            e.printStackTrace();
            quitCamera();
//            toast("打开相机失败，有可能没有相关权限，请在\"设置\"中打开相机权限");
            Log.e(TAG, "[CAMERA ID] open = " + e.toString());
//                throw new RuntimeException("setup camera");
        }
    }

    private int getCurrentId() {
        return mCameraId;
    }

    public boolean isHorScreen() {
        Configuration mConfiguration = mContext.getResources().getConfiguration(); //获取设置的配置信息
        int ori = mConfiguration.orientation; //获取屏幕方向
        if (ori == Configuration.ORIENTATION_LANDSCAPE) {
            return true;
        } else if (ori == Configuration.ORIENTATION_PORTRAIT) {
            return false;
        }
        return false;
    }
}
