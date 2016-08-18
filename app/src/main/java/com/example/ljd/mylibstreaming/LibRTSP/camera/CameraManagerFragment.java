package com.example.ljd.mylibstreaming.LibRTSP.camera;

/**
 * Created by ljd-pc on 2016/7/13.
 */

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.MediaCodec;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v13.app.FragmentCompat;
import android.support.v4.app.ActivityCompat;
import android.support.v4.util.LruCache;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.ljd.mylibstreaming.LibRTSP.session.Session;
import com.example.ljd.mylibstreaming.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CameraManagerFragment extends Fragment
        implements FragmentCompat.OnRequestPermissionsResultCallback {

    private boolean VERBOSE = false;
    private static volatile CameraManagerFragment cameraManagerFragment;

    public static CameraManagerFragment getInstance(){
        if(cameraManagerFragment == null){
            synchronized (CameraManagerFragment.class){
                if(cameraManagerFragment == null){
                    cameraManagerFragment = new CameraManagerFragment();
                }
            }
        }
        return cameraManagerFragment;
    }



    private static final String TAG = "CameraManagerFragment";

    private static final int REQUEST_VIDEO_PERMISSIONS = 1;
    private static final String FRAGMENT_DIALOG = "dialog";
    List<Surface> MediaCodecInputSurfaces= new ArrayList<>();
    List<Surface> CaptureSessionInputSurfaces = new ArrayList<>();

    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    private Session session;

    private static final String[] VIDEO_PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
    };


    /**
     * An {@link AutoFitTextureView} for camera preview.
     */
    private AutoFitTextureView mTextureView;


    /**
     * A refernce to the opened {@link android.hardware.camera2.CameraDevice}.
     */
    private CameraDevice mCameraDevice;

    /**
     * A reference to the current {@link android.hardware.camera2.CameraCaptureSession} for
     * preview.
     */
    private CameraCaptureSession mPreviewSession;

//    /**
//     * {@link TextureView.SurfaceTextureListener} handles several lifecycle events on a
//     * {@link TextureView}.
//     */
//    private TextureView.SurfaceTextureListener mSurfaceTextureListener
//            = new TextureView.SurfaceTextureListener() {
//
//        @Override
//        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture,
//                                              int width, int height) {
//            openCamera(width, height);
//        }
//
//        @Override
//        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture,
//                                                int width, int height) {
//            configureTransform(width, height);
//        }
//
//        @Override
//        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
//            return true;
//        }
//
//        @Override
//        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
//        }
//
//    };

    /**
     * The {@link android.util.Size} of camera preview.
     */
    private Size mPreviewSize;

    public Size getVideoSize() {
        return mVideoSize;
    }

    public void setVideoSize(Size mVideoSize) {
        this.mVideoSize = mVideoSize;
    }

    /**
     * The {@link android.util.Size} of video recording.
     */
    private Size mVideoSize;

    /**
     * MediaRecorder
     */
    private MediaRecorder mMediaRecorder;

    /**
     * Whether the app is recording video now
     */


    /**
     * An additional thread for running tasks that shouldn't block the UI.
     */
    private HandlerThread mBackgroundThread;

    /**
     * A {@link Handler} for running tasks in the background.
     */
    private Handler mBackgroundHandler;

    /**
     * A {@link Semaphore} to prevent the app from exiting before closing the camera.
     */
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);


    /**
     * {@link CameraDevice.StateCallback} is called when {@link CameraDevice} changes its status.
     */

    //摄像头状态回调函数
    //manager.openCamera 调用
    //当摄像头状态改变时就执行
    private CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(CameraDevice cameraDevice) {
            if(VERBOSE) Log.v(TAG,"CameraDevice.StateCallback -> onOpened(CameraDevice cameraDevice)");
            mCameraDevice = cameraDevice;
            //开始预览
            startPreview();
            mCameraOpenCloseLock.release();
            if (null != mTextureView) {
                configureTransform(mTextureView.getWidth(), mTextureView.getHeight());
            }
        }

        @Override
        public void onDisconnected(CameraDevice cameraDevice) {
            if(VERBOSE) Log.v(TAG,"CameraDevice.StateCallback -> onDisconnected(CameraDevice cameraDevice)");
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(CameraDevice cameraDevice, int error) {
            if(VERBOSE) Log.v(TAG,"CameraDevice.StateCallback -> onError(CameraDevice cameraDevice, int error)");
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
            Activity activity = getActivity();
            if (null != activity) {
                activity.finish();
            }
        }

    };
    private Integer mSensorOrientation;
    private String mNextVideoAbsolutePath;
    private CaptureRequest.Builder mPreviewBuilder;


//    public static CameraManagerFragment newInstance() {
//        return new CameraManagerFragment();
//    }


    /**
     * Given {@code choices} of {@code Size}s supported by a camera, chooses the smallest one whose
     * width and height are at least as large as the respective requested values, and whose aspect
     * ratio matches with the specified value.
     *
     *
     * @return The optimal {@code Size}, or an arbitrary one if none were big enough
     */


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if(VERBOSE) Log.v(TAG,"public View onCreateView");
        return inflater.inflate(R.layout.preview_fragment, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        if(VERBOSE) Log.v(TAG,"public void onViewCreated");
        mTextureView = (AutoFitTextureView) view.findViewById(R.id.texture);
    }

    @Override
    public void onResume() {
        if(VERBOSE) Log.v(TAG,"public void onResume()");
        super.onResume();
        startBackgroundThread();
        /*打开摄像头，设置摄像头状态回调
        一般情况下mTextureView都是不能准备好的
        所以这一个判断很必要
        */
        if (mTextureView.isAvailable()) {
            if(VERBOSE) Log.v(TAG,"mTextureView.IS Available");
            openCamera(mTextureView.getWidth(), mTextureView.getHeight());
        } else {
            if(VERBOSE) Log.v(TAG,"mTextureView NOT Available");
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }

    }

    public void OpenTheCamera(){
        if(VERBOSE) Log.v(TAG,"public void OpenTheCamera()");
        openCamera(mTextureView.getWidth(), mTextureView.getHeight());
    }

    @Override
    public void onPause() {
        if(VERBOSE) Log.v(TAG,"public void onPause()");
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    public void StopCameraManager(){
        closeCamera();
        stopBackgroundThread();
    }

//    @Override
//    public void onClick(View view) {
//        switch (view.getId()) {
//            case R.id.video: {
//                if (mIsRecordingVideo) {
//                    stopRecordingVideo();
//                } else {
//                    startRecordingVideo();
//                }
//                break;
//            }
//            case R.id.info: {
//                Activity activity = getActivity();
//                if (null != activity) {
//                    new AlertDialog.Builder(activity)
//                            .setMessage(" This sample demonstrates how to record video using Camera2 API.")
//                            .setPositiveButton(android.R.string.ok, null)
//                            .show();
//                }
//                break;
//            }
//        }
//    }

    /**
     * Starts a background thread and its {@link Handler}.
     */
    private void startBackgroundThread() {
        if(VERBOSE) Log.v(TAG,"private void startBackgroundThread()");
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    private void stopBackgroundThread() {
        if(VERBOSE) Log.v(TAG,"private void stopBackgroundThread()");
        if(mBackgroundThread != null) {
            mBackgroundThread.quitSafely();
            try {
                mBackgroundThread.join();
                mBackgroundThread = null;
                mBackgroundHandler = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * Gets whether you should show UI with rationale for requesting permissions.
     *
     * @param permissions The permissions your app wants to request.
     * @return Whether you can show permission rationale UI.
     */
    private boolean shouldShowRequestPermissionRationale(String[] permissions) {
        for (String permission : permissions) {
            if (FragmentCompat.shouldShowRequestPermissionRationale(this, permission)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Requests permissions needed for recording video.
     */
    private void requestVideoPermissions() {
        if (shouldShowRequestPermissionRationale(VIDEO_PERMISSIONS)) {
            new ConfirmationDialog().show(getChildFragmentManager(), FRAGMENT_DIALOG);
        } else {
            FragmentCompat.requestPermissions(this, VIDEO_PERMISSIONS, REQUEST_VIDEO_PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult");
        if (requestCode == REQUEST_VIDEO_PERMISSIONS) {
            if (grantResults.length == VIDEO_PERMISSIONS.length) {
                for (int result : grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) {
                        ErrorDialog.newInstance("permission_request")
                                .show(getChildFragmentManager(), FRAGMENT_DIALOG);
                        break;
                    }
                }
            } else {
                ErrorDialog.newInstance("permission_request")
                        .show(getChildFragmentManager(), FRAGMENT_DIALOG);
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private boolean hasPermissionsGranted(String[] permissions) {
        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(getActivity(), permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
    private static Size chooseVideoSize(Size[] choices) {
        for (Size size : choices) {
            if (size.getWidth() == size.getHeight() * 4 / 3 && size.getWidth() <= 1080) {
                return size;
            }
        }
        Log.e(TAG, "Couldn't find any suitable video size");
        return choices[choices.length - 1];
    }
    private static Size chooseOptimalSize(Size[] choices, int width, int height, Size aspectRatio) {
        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<Size>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getHeight() == option.getWidth() * h / w &&
                    option.getWidth() >= width && option.getHeight() >= height) {
                bigEnough.add(option);
            }
        }

        // Pick the smallest of those, assuming we found any
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else {
            Log.e(TAG, "Couldn't find any suitable preview size");
            return choices[0];
        }
    }

    /**
     * Tries to open a {@link CameraDevice}. The result is listened by `mStateCallback`.
     */
    private void openCamera(int width, int height) {
        if(VERBOSE)Log.v(TAG,"private void openCamera(int width, int height)");
        if (!hasPermissionsGranted(VIDEO_PERMISSIONS)) {
            requestVideoPermissions();
            return;
        }
        final Activity activity = getActivity();
        if (null == activity || activity.isFinishing()) {
            return;
        }
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            Log.d(TAG, "tryAcquire");
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }

            String cameraId = manager.getCameraIdList()[0];
            Log.d(TAG, "cameraId = "+cameraId);
            // Choose the sizes for camera preview and video recording
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics
                    .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
            mVideoSize = chooseVideoSize(map.getOutputSizes(MediaCodec.class));
            mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                    width, height,mVideoSize );
            //mPreviewSize = new Size(session.getVideoQuality().getmWidth(), session.getVideoQuality().getmHeight());
            //mPreviewSize = new Size(100, 100);

            int orientation = getResources().getConfiguration().orientation;
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                mTextureView.setAspectRatio(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            } else {
                mTextureView.setAspectRatio(mPreviewSize.getHeight(), mPreviewSize.getWidth());
            }
            configureTransform(width, height);
            //mMediaRecorder = new MediaRecorder();


            if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                Log.e(TAG,"没有相机权限");
                return;
            }
            manager.openCamera(cameraId, mStateCallback, null);
        } catch (CameraAccessException e) {
            Toast.makeText(activity, "Cannot access the camera.", Toast.LENGTH_SHORT).show();
            activity.finish();
        } catch (NullPointerException e) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
            ErrorDialog.newInstance("camera_error")
                    .show(getChildFragmentManager(), FRAGMENT_DIALOG);
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.");
        }
    }

    private void closeCamera() {
        if(VERBOSE)Log.v(TAG,"private void closeCamera()");
        try {
            mCameraOpenCloseLock.acquire();
            closePreviewSession();
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (null != mMediaRecorder) {
                mMediaRecorder.release();
                mMediaRecorder = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.");
        } finally {
            mCameraOpenCloseLock.release();
        }
    }

    /**
     * Start the camera preview.
     */
    private void startPreview() {
        if(VERBOSE)Log.v(TAG,"private void startPreview()");
        if (null == mCameraDevice || !mTextureView.isAvailable() || null == mPreviewSize) {
            return;
        }
        try {
            closePreviewSession();
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

            Surface previewSurface = new Surface(texture);
            mPreviewBuilder.addTarget(previewSurface);
            //后台线程执行
            mCameraDevice.createCaptureSession(Arrays.asList(previewSurface), new CameraCaptureSession.StateCallback() {

                @Override
                public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                    mPreviewSession = cameraCaptureSession;
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
                    Activity activity = getActivity();
                    if (null != activity) {
                        Toast.makeText(activity, "Failed", Toast.LENGTH_SHORT).show();
                    }
                }
            }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Update the camera preview. {@link #startPreview()} needs to be called in advance.
     */
    private void updatePreview() {
        if(VERBOSE)Log.v(TAG,"private void updatePreview()");
        if (null == mCameraDevice) {
            return;
        }
        try {
            setUpCaptureRequestBuilder(mPreviewBuilder);
            HandlerThread thread = new HandlerThread("CameraPreview");
            thread.start();
            //后台线程执行
            mPreviewSession.setRepeatingRequest(mPreviewBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void setUpCaptureRequestBuilder(CaptureRequest.Builder builder) {
        if(VERBOSE)Log.v(TAG,"private void setUpCaptureRequestBuilder");
        builder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
    }

    /**
     * Configures the necessary {@link android.graphics.Matrix} transformation to `mTextureView`.
     * This method should not to be called until the camera preview size is determined in
     * openCamera, or until the size of `mTextureView` is fixed.
     *
     * @param viewWidth  The width of `mTextureView`
     * @param viewHeight The height of `mTextureView`
     */
    private void configureTransform(int viewWidth, int viewHeight) {
        if(VERBOSE)Log.v(TAG,"private void configureTransform");
        Activity activity = getActivity();
        if (null == mTextureView || null == mPreviewSize || null == activity) {
            return;
        }
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / mPreviewSize.getHeight(),
                    (float) viewWidth / mPreviewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        }
        mTextureView.setTransform(matrix);
    }



    private String getVideoFilePath(Context context) {
        return context.getExternalFilesDir(null).getAbsolutePath() + "/"
                + System.currentTimeMillis() + ".mp4";
    }

    public void SetMediaCodecInputSurface(Surface mRecorderSurface) {
        if (null == mCameraDevice || !mTextureView.isAvailable() || null == mPreviewSize) {
            return;
        }
        try {
            closePreviewSession();
            /////////////////////////////////////////////
            //屏幕预览 surface
            //////////////////////////////////////
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);

            // Set up Surface for the camera preview
            Surface previewSurface = new Surface(texture);
            CaptureSessionInputSurfaces.clear();//清空输入surface
            CaptureSessionInputSurfaces.add(previewSurface);//加入屏幕预览surface
            mPreviewBuilder.addTarget(previewSurface);//屏幕预览surface，加入CaptureRequest.Builder
            ////////////////////////////////////////////////////////
            //视频录制 surface
            ////////////////////////////////////////////////////
            // Set up Surface for the MediaRecorder
            MediaCodecInputSurfaces.add(mRecorderSurface);//每一个新的连接都会加入一个mRecorderSurface
            //mPreviewBuilder.addTarget(mRecorderSurface);
            for(Surface surface:MediaCodecInputSurfaces){
                CaptureSessionInputSurfaces.add(surface);
                mPreviewBuilder.addTarget(surface);//包括编码器的surface加入CaptureRequest.Builder
            }

            // Start a capture session
            // Once the session starts, we can update the UI and start recording

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    public void StartCreateData(){
        try {
            //后台线程执行
            //将surface的list送入
            mCameraDevice.createCaptureSession(CaptureSessionInputSurfaces, new CameraCaptureSession.StateCallback() {

                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    if(VERBOSE)Log.v(TAG,"StartCreateData() -> onConfigured");
                    mPreviewSession = cameraCaptureSession;
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    if(VERBOSE)Log.e(TAG,"StartCreateData() -> onConfigureFailed");
                }
            }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void closePreviewSession() {
        if(VERBOSE)Log.v(TAG,"private void closePreviewSession()");
        if (mPreviewSession != null) {
            mPreviewSession.close();
            mPreviewSession = null;
        }
    }

//    public void stopRecordingVideo() {
//        // UI
//        // Stop recording
//        mMediaRecorder.stop();
//        mMediaRecorder.reset();
//
//        Activity activity = getActivity();
//        if (null != activity) {
//            Toast.makeText(activity, "Video saved: " + mNextVideoAbsolutePath,
//                    Toast.LENGTH_SHORT).show();
//            Log.d(TAG, "Video saved: " + mNextVideoAbsolutePath);
//        }
//        mNextVideoAbsolutePath = null;
    //停止录制后，开启预览
//        startPreview();
//    }

    /**
     * Compares two {@code Size}s based on their areas.
     */
    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }

    }

    public static class ErrorDialog extends DialogFragment {

        private static final String ARG_MESSAGE = "message";

        public static ErrorDialog newInstance(String message) {
            ErrorDialog dialog = new ErrorDialog();
            Bundle args = new Bundle();
            args.putString(ARG_MESSAGE, message);
            dialog.setArguments(args);
            return dialog;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Activity activity = getActivity();
            return new AlertDialog.Builder(activity)
                    .setMessage(getArguments().getString(ARG_MESSAGE))
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            activity.finish();
                        }
                    })
                    .create();
        }

    }

    public static class ConfirmationDialog extends DialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Fragment parent = getParentFragment();
            return new AlertDialog.Builder(getActivity())
                    .setMessage("permission_request")
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            FragmentCompat.requestPermissions(parent, VIDEO_PERMISSIONS,
                                    REQUEST_VIDEO_PERMISSIONS);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    parent.getActivity().finish();
                                }
                            })
                    .create();
        }

    }
    private TextureView.SurfaceTextureListener mSurfaceTextureListener
            = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture,
                                              int width, int height) {
            if(VERBOSE)Log.v(TAG,"TextureView.SurfaceTextureListener -> onSurfaceTextureAvailable");
            openCamera(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture,
                                                int width, int height) {
            if(VERBOSE)Log.v(TAG,"TextureView.SurfaceTextureListener -> onSurfaceTextureSizeChanged");
            configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
        }

    };
}
