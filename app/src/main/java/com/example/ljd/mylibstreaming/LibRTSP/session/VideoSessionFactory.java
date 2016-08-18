package com.example.ljd.mylibstreaming.LibRTSP.session;

import android.media.projection.MediaProjection;
import android.util.Log;

import com.example.ljd.mylibstreaming.LibRTSP.quality.VideoQuality;

/**
 * Created by ljd-pc on 2016/7/3.
 */
public class VideoSessionFactory extends AbstractSessionFactory{

    private String TAG = "VideoSessionFactory";
    private static volatile VideoSessionFactory videoSessionFactory;
    private VideoSessionFactory(){}
    public static VideoSessionFactory getInstance(){
        if(videoSessionFactory ==null){
            synchronized (VideoSessionFactory.class){
                if(videoSessionFactory == null){
                    videoSessionFactory = new VideoSessionFactory();
                }
            }
        }
        return videoSessionFactory;
    }


    @Override
    public Session CreatSession(int type,String mVideoPath, String mDestination, int mDestinationPort,
                      VideoQuality mVideoQuality, int mTimeToLive,
                      String mOrigin, int mOriginPort, MediaProjection mMediaProjection) {
        Session session = mSessions.get(type);
        if(session == null) {
            Log.i(TAG,"新建一个session");
            session = new Session(type, mVideoPath, mDestination, mDestinationPort,
                    mVideoQuality, mTimeToLive,
                    mOrigin, mOriginPort, mMediaProjection);
            mSessions.put(type,session);
        }
        return session;
    }
}
