package com.example.ljd.mylibstreaming;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.camera2.CameraManager;
import android.media.MediaMetadataRetriever;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.example.ljd.mylibstreaming.LibRTSP.camera.CameraManagerFragment;
import com.example.ljd.mylibstreaming.LibRTSP.quality.VideoQuality;
import com.example.ljd.mylibstreaming.LibRTSP.rtsp.RtspServer;
import com.example.ljd.mylibstreaming.LibRTSP.session.Session;
import com.example.ljd.mylibstreaming.LibRTSP.utility.RunState;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private boolean VERBOSE = true;
    private MediaProjectionManager mMediaProjectionManager;
    public static MediaProjection mMediaProjection;
    ToggleButton tbtScreenCaptureService;
    private ScreenCaptureService.MyBinder mBinder;

    private static final int CAPTURE_CODE = 115;
    private static final int WRITE_EXTERNAL_STORAGE_REQUEST_CODE = 123;
    private static final int INTERNET_REQUEST_CODE = 124;
    private static final int CAMERA_REQUEST_CODE = 125;

    private int mFileDensity;
    private int mFileWidth;
    private int mFileHeight;

    private int mScreenDensity;
    private int mScreenWidth;
    private int mScreenHeight;

    private int mDestinationPort = 5006;
    private int mOriginPort = 1234;

    private static final String SDCARD_PATH  = Environment.getExternalStorageDirectory().getPath();
    private String VIDEO_PATH;
    private int SESSION_TYPE = 0;
    private int TYPE_VIDEO_H264 = 1;//屏幕录制推流功能
    private int TYPE_VIDEO_CAMERA = 2;//摄像头推流功能
    private int TYPE_VIDEO_MP4_FILE = 3;//本地视频文件推流功能
    private Session session;

    private VideoQuality screenVideoQuality;
    private VideoQuality fileVideoQulity;

    private CameraManager mCameraManager;
    private CameraManagerFragment camera2VideoFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //设置视频文件路径
        VIDEO_PATH = SDCARD_PATH+"/ljd/mp4/dxflqm.mp4";

        InitUI();
        UIListener();
        if (!RunState.getInstance().isRun()) {
            Log.v(TAG,"如果刚启动的话");
            AskForPermission();
            GetMediaInfo();
            GetMediaInfo();
            NewSession();
        }
        myBindService();
    }

    private void InitUI(){
        tbtScreenCaptureService = (ToggleButton) findViewById(R.id.tbt_screen_capture_service);
        camera2VideoFragment = CameraManagerFragment.getInstance();
        getFragmentManager().beginTransaction()
                    .replace(R.id.container,camera2VideoFragment )
                    .commit();


    }

    private void UIListener(){
        tbtScreenCaptureService.setChecked(RunState.getInstance().isRun());
        tbtScreenCaptureService.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (RunState.getInstance().isRun()) {
                    mBinder.StopScreenCapture();
                    RunState.getInstance().setRun(false);
                    Toast.makeText(MainActivity.this, "屏幕录制服务停止运行", Toast.LENGTH_SHORT).show();
                } else {
                    //SetSession();
                    myShareScreen();
                    RunState.getInstance().setRun(true);
                    Toast.makeText(MainActivity.this, "屏幕录制服务开始运行", Toast.LENGTH_SHORT).show();
                }
            }

        });
    }


    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.v(TAG,"onActivityResult");
        if(requestCode!=CAPTURE_CODE){
            Log.e(TAG,"onActivityResult : requestCode!=CAPTURE_CODE");
            return;
        }
        if (resultCode != RESULT_OK) {
            Log.e(TAG,"onActivityResult : resultCode != RESULT_OK");
            mMediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            startActivityForResult(mMediaProjectionManager.createScreenCaptureIntent(), CAPTURE_CODE);
        }
        mMediaProjection = mMediaProjectionManager.getMediaProjection(resultCode,data);
        session.setMediaProjection(mMediaProjection);
    }
    private void AskForPermission(){
        if (Build.VERSION.SDK_INT >= 23) {
            Log.v(TAG, "AskForPermission()");
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.v("AskForPermission()", "requestPermissions");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_EXTERNAL_STORAGE_REQUEST_CODE);
            }
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.INTERNET)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.v("AskForPermission()", "requestPermissions");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET}, INTERNET_REQUEST_CODE);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.v("AskForPermission()", "requestPermissions");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.v("PermissionsResult","onRequestPermissionsResult");
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    //绑定服务时调用
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBinder = (ScreenCaptureService.MyBinder)service;
        }
        //解绑时不会调用
        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    private RtspServer.CallbackListener mRtspCallbackListener = new RtspServer.CallbackListener() {

        @Override
        public void onError(RtspServer server, Exception e, int error) {
            // We alert the user that the port is already used by another app.
            if (error == RtspServer.ERROR_BIND_FAILED) {

            }
        }

        @Override
        public void onMessage(RtspServer server, int message) {
        }

    };

    private void GetWindowInfo(){
        mMediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        startActivityForResult(mMediaProjectionManager.createScreenCaptureIntent(), CAPTURE_CODE);
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager mWindowManager = (WindowManager)getApplication().getSystemService(getApplication().WINDOW_SERVICE);
        mWindowManager.getDefaultDisplay().getMetrics(metrics);
        mScreenDensity = metrics.densityDpi;
        mScreenWidth = metrics.widthPixels/2;
        mScreenHeight = metrics.heightPixels/2;
        //mScreenWidth = 1024;
        //mScreenHeight = 768;
        Log.v(TAG,"mScreenWidth is :"+mScreenWidth+";mScreenHeight is :"+mScreenHeight+"mScreenDensity is :"+mScreenDensity);

    }

    private void GetMP4Info(){
        MediaMetadataRetriever retr = new MediaMetadataRetriever();
        try {
            retr.setDataSource(VIDEO_PATH);
        }catch (Exception e){
            return;
        }
        Bitmap bm = retr.getFrameAtTime();
        mFileWidth = bm.getWidth();
        mFileHeight = bm.getHeight();
        mFileDensity = bm.getDensity();
        Log.v(TAG,"mScreenWidth is :"+mScreenWidth+";mScreenHeight is :"+mScreenHeight+"mScreenDensity is :"+mScreenDensity);
    }

    private void GetCameraInfo(){

    }
    private void GetMediaInfo(){
        Log.v(TAG,"GetMediaInfo()");
            GetWindowInfo();
            GetMP4Info();
            GetCameraInfo();
    }

    private void SetSession(){
//        Log.v(TAG,"SetSessionBuilder()");
//        screenVideoQuality = new VideoQuality(mScreenWidth,mScreenHeight,30,8000000,mScreenDensity);
//        fileVideoQulity = new VideoQuality(mFileWidth,mFileHeight,30,8000000,mFileDensity);
//        session = new Session(SESSION_TYPE,VIDEO_PATH,null,mDestinationPort,
//                new VideoQuality(mScreenWidth,mScreenHeight,30,8000000,mScreenDensity),200,
//                null,mOriginPort,null);
//        session.setScreenVideoQuality(screenVideoQuality);
//        session.setFileVideoQulity(fileVideoQulity);
        session.setMediaProjection(mMediaProjection);
//        camera2VideoFragment.setSession(session);

    }

    private void NewSession(){
        Log.v(TAG,"NewSession()");
        screenVideoQuality = new VideoQuality(mScreenWidth,mScreenHeight,30,8000000,mScreenDensity);
        fileVideoQulity = new VideoQuality(mFileWidth,mFileHeight,30,8000000,mFileDensity);
        session = new Session(0,VIDEO_PATH,null,mDestinationPort,
                screenVideoQuality,200,
                null,mOriginPort,null);
        session.setScreenVideoQuality(screenVideoQuality);
        session.setFileVideoQulity(fileVideoQulity);
    }

    private void myShareScreen(){
        myStartService();
        mBinder.StartScreenCapture(session);//启动服务
    }

    @Override
    protected void onDestroy() {

        myUnbindService();
        if(!RunState.getInstance().isRun()){
            myStopService();

        }

        camera2VideoFragment.StopCameraManager();

        super.onDestroy();

    }

    private void myBindService(){
        Intent bindIntent = new Intent(MainActivity.this, ScreenCaptureService.class);
        bindService(bindIntent, connection, BIND_AUTO_CREATE);
        Log.v(TAG,"bindService");
    }

    private void myUnbindService(){
            unbindService(connection);
            Log.v(TAG,"unbindService");
    }

    private void myStartService(){
        Intent intent = new Intent(MainActivity.this, ScreenCaptureService.class);
        startService(intent);
        Log.v(TAG,"startService");
    }

    private void myStopService(){
        Intent stopIntent = new Intent(MainActivity.this, ScreenCaptureService.class);
        stopService(stopIntent);
        releaseEncoder();
    }


    private void releaseEncoder() {
        Log.d(TAG, "releasing mMediaProjection objects");

        if(mMediaProjection!=null){
            mMediaProjection.stop();
            mMediaProjection = null;
        }
        if(mMediaProjectionManager!=null){

            mMediaProjectionManager = null;
        }
    }


}
