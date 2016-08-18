package com.example.ljd.mylibstreaming.LibRTSP.session;

import android.media.projection.MediaProjection;

import com.example.ljd.mylibstreaming.LibRTSP.encorder.video.VideoEncorder;
import com.example.ljd.mylibstreaming.LibRTSP.stream.Stream;
import com.example.ljd.mylibstreaming.LibRTSP.quality.VideoQuality;

import java.util.WeakHashMap;

/**
 * Created by ljd-pc on 2016/7/3.
 */
abstract public class AbstractSessionFactory {
    VideoEncorder mVideoEncorder;
    Stream mStream;
    int type;
    protected WeakHashMap<Integer,Session> mSessions = new WeakHashMap<Integer,Session>();
    public abstract Session CreatSession(int type,String mVideoPath, String mDestination, int mDestinationPort,
                               VideoQuality mVideoQuality, int mTimeToLive,
                               String mOrigin, int mOriginPort, MediaProjection mMediaProjection);
}
