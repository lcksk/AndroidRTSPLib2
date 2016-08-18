package com.example.ljd.mylibstreaming;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.example.ljd.mylibstreaming.LibRTSP.rtsp.RtspServer;
import com.example.ljd.mylibstreaming.LibRTSP.session.Session;
import com.example.ljd.mylibstreaming.LibRTSP.utility.RunState;

/**
 * Created by ljd-pc on 2016/7/8.
 */
public class ScreenCaptureService extends RtspServer {
    public static final String TAG = "ScreenCaptureService";
    //

    public ScreenCaptureService(){
        super();
    }

    public ScreenCaptureService(Session mSession){
        super(mSession);
    }

    private MyBinder mBinder = new MyBinder();
    @Override
    public void onCreate() {
        super.onCreate();
        Notification.Builder builder = new Notification.Builder(this);
        Intent notificationIntent = new Intent(this,MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,0,notificationIntent,0);
        builder.setContentIntent(pendingIntent);
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setTicker("屏幕录像前台服务开始");
        builder.setContentTitle("屏幕录像前台服务");
        builder.setContentText("屏幕录像前台服务正在运行");
        Notification notification = builder.build();
        startForeground(1,notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    class MyBinder extends Binder {
        public void StartScreenCapture(Session session){
            Log.v(TAG,"StartScreenCapture()");
            setmSession(session);
            start();

            return;
        }

        public void StopScreenCapture(){
            Log.v(TAG,"StopScreenCapture()");
        }

    }
}
