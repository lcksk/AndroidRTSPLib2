package com.example.ljd.mylibstreaming.LibRTSP.stream.video;

import com.example.ljd.mylibstreaming.LibRTSP.quality.VideoQuality;
import com.example.ljd.mylibstreaming.LibRTSP.session.Session;
import com.example.ljd.mylibstreaming.LibRTSP.stream.MediaStream;

/**
 * Created by ljd-pc on 2016/6/22.
 */
public abstract class VideoStream extends MediaStream {
    protected final static String TAG = "VideoStream";
    protected VideoQuality mRequestedQuality = VideoQuality.DEFAULT_VIDEO_QUALITY.clone();
    protected String mMimeType;
    public VideoStream(Session mSession){
        super(mSession);
        mRequestedQuality = mSession.getVideoQuality();
    }


    public void setVideoQuality(VideoQuality videoQuality) {
        mRequestedQuality = videoQuality;
    }

    public VideoQuality getVideoQuality() {
        return mRequestedQuality;
    }

}
