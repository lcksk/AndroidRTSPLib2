package com.example.ljd.mylibstreaming.LibRTSP.session;

import android.content.Context;
import android.hardware.camera2.CameraManager;
import android.media.projection.MediaProjection;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.view.SurfaceView;

import com.example.ljd.mylibstreaming.LibRTSP.quality.VideoQuality;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.WeakHashMap;


/**
 * Created by ljd-pc on 2016/6/22.
 * session 用于配置各种信息
 * 单例模式
 */
public class Session {

    private static final String TAG = "Session";
    private String mVideoPath;//等待发送的视频文件路径
    private int mDestinationPort;//接收端地址端口号
    private String mDestination;//接收端地址
    private InetAddress mDestinationAddress;//接收端地址
    private VideoQuality mVideoQuality;//视频流的相关信息，如 宽、高、码率、帧率
    private int mTimeToLive;//TTL,在路由器中转发的最大次数
    private String mOrigin;//服务器端地址
    private int mOriginPort;//服务器端端口号


    private VideoQuality screenVideoQuality;
    private VideoQuality fileVideoQulity;
    private int sessionType = 0;
    private int TYPE_VIDEO_H264 = 1;
    private int TYPE_VIDEO_CAMERA = 2;
    private int TYPE_VIDEO_MP4_FILE = 3;
    //private int TYPE_AUDIO_ = ...




    private Context mContext;
    private MediaProjection mMediaProjection;

    private volatile static Session session;
    private Handler mMainHandler;

    private Handler mHandler;
    private long mTimestamp;



    private SurfaceView surfaceView;

    private CameraManager cameraManager;
    public Session(){

    }

    public Session(int type,String mVideoPath,String mDestination,int mDestinationPort,
                           VideoQuality mVideoQuality,int mTimeToLive,
                   String mOrigin,int mOriginPort,MediaProjection mMediaProjection){
        this.sessionType = type;
        this.mVideoPath = mVideoPath;
        this.mDestination = mDestination;
        this.mDestinationPort = mDestinationPort;
        this.mVideoQuality = mVideoQuality;
        this.mTimeToLive = mTimeToLive;
        this.mOrigin = mOrigin;
        this.mOriginPort = mOriginPort;
        this.mMediaProjection = mMediaProjection;
        try {
            mDestinationAddress = InetAddress.getByName(mDestination);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        long uptime = System.currentTimeMillis();

        HandlerThread thread = new HandlerThread("com.example.ljd.mylibstreaming.LibRTSP.session.Session");
        thread.start();

        mHandler = new Handler(thread.getLooper());
        mMainHandler = new Handler(Looper.getMainLooper());
        mTimestamp = (uptime/1000)<<32 & (((uptime-((uptime/1000)*1000))>>32)/1000); // NTP timestamp
    }
    public Session(int type,
                   VideoQuality mVideoQuality,int mTimeToLive,
                   int mOriginPort,MediaProjection mMediaProjection){
        this.sessionType = type;
        this.mVideoQuality = mVideoQuality;
        this.mTimeToLive = mTimeToLive;
        this.mOriginPort = mOriginPort;
        this.mMediaProjection = mMediaProjection;
    }

    public String getVideoPath() {
        return mVideoPath;
    }

    public void setVideoPath(String videoPath) {
        mVideoPath = videoPath;
    }

    public String getDestination() {
        return mDestination;
    }

    public void setDestination(String destination) {
        mDestination = destination;
        try {
            mDestinationAddress = InetAddress.getByName(mDestination);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public int getDestinationPort() {
        return mDestinationPort;
    }

    public void setDestinationPort(int mDestinationPort) {
        this.mDestinationPort = mDestinationPort;
    }

    public int getTimeToLive() {
        return mTimeToLive;
    }

    public void setTimeToLive(int mTimeToLive) {
        this.mTimeToLive = mTimeToLive;
    }

    public String getOrigin() {
        return mOrigin;
    }

    public void setOrigin(String mOrigin) {
        this.mOrigin = mOrigin;
    }

    public int getOriginPort() {
        return mOriginPort;
    }

    public void setOriginPort(int mOriginPort) {
        this.mOriginPort = mOriginPort;
    }

    public VideoQuality getVideoQuality() {
        return mVideoQuality;
    }

    public void setVideoQulity(VideoQuality mVideoQuality) {
        this.mVideoQuality = mVideoQuality;
    }

    public Context getContext() {
        return mContext;
    }

    public void setContext(Context mContext) {
        this.mContext = mContext;
    }

    public MediaProjection getMediaProjection() {
        return mMediaProjection;
    }

    public void setMediaProjection(MediaProjection mMediaProjection) {
        this.mMediaProjection = mMediaProjection;
    }
    public InetAddress getDestinationAddress() {
        return mDestinationAddress;
    }

    public void setDestinationAddress(InetAddress mDestinationAddress) {
        this.mDestinationAddress = mDestinationAddress;
    }
    public int getSessionType() {
        return sessionType;
    }

    public void setSessionType(int sessionType) {
        this.sessionType = sessionType;
    }
    public CameraManager getCameraManager() {
        return cameraManager;
    }

    public void setCameraManager(CameraManager cameraManager) {
        this.cameraManager = cameraManager;
    }

    public SurfaceView getSurfaceView() {
        return surfaceView;
    }

    public void setSurfaceView(SurfaceView surfaceView) {
        this.surfaceView = surfaceView;
    }

    public VideoQuality getScreenVideoQuality() {
        return screenVideoQuality;
    }

    public void setScreenVideoQuality(VideoQuality screenVideoQuality) {
        this.screenVideoQuality = screenVideoQuality;
    }

    public VideoQuality getFileVideoQulity() {
        return fileVideoQulity;
    }

    public void setFileVideoQulity(VideoQuality fileVideoQulity) {
        this.fileVideoQulity = fileVideoQulity;
    }

    /**
     * Returns a Session Description that can be stored in a file or sent to a client with RTSP.
     * @return The Session Description.
     * @throws IllegalStateException Thrown when {@link #setDestination(String)} has never been called.
     */
    public String getSessionDescription() {
        StringBuilder sessionDescription = new StringBuilder();
        if (mDestination==null) {
            throw new IllegalStateException("setDestination() has not been called !");
        }
        sessionDescription.append("v=0\r\n");
        // TODO: Add IPV6 support
        sessionDescription.append("o=- "+mTimestamp+" "+mTimestamp+" IN IP4 "+mOrigin+"\r\n");
        sessionDescription.append("s=Unnamed\r\n");
        sessionDescription.append("i=N/A\r\n");
        sessionDescription.append("c=IN IP4 "+mDestination+"\r\n");
        // t=0 0 means the session is permanent (we don't know when it will stop)
        sessionDescription.append("t=0 0\r\n");
        sessionDescription.append("a=recvonly\r\n");
        // Prevents two different sessions from using the same peripheral at the same time
        return sessionDescription.toString();
    }

}
