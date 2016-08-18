package com.example.ljd.mylibstreaming.LibRTSP.stream.video;

import android.media.MediaCodec;
import android.media.MediaExtractor;

import com.example.ljd.mylibstreaming.LibRTSP.session.Session;
import com.example.ljd.mylibstreaming.LibRTSP.stream.AbstractStreamFactory;
import com.example.ljd.mylibstreaming.LibRTSP.stream.MediaStream;

/**
 * Created by ljd-pc on 2016/7/4.
 */
public class VideoStreamFactory extends AbstractStreamFactory {

    private static volatile VideoStreamFactory videoStreamFactory;
    private VideoStreamFactory(){}

    public static VideoStreamFactory getInstance(){
        if(videoStreamFactory == null){
            synchronized (VideoStreamFactory.class){
                if(videoStreamFactory == null){
                    videoStreamFactory = new VideoStreamFactory();
                }
            }
        }
        return videoStreamFactory;
    }

    public MediaStream CreateStream(MediaCodec mMediaCodec, MediaExtractor mediaExtractor,Session mSession){
        if(mSession.getSessionType() == 1) {
            return new H264Stream(mMediaCodec, mSession);
        }
        if(mSession.getSessionType() == 2) {
            return new H264Stream(mMediaCodec, mSession);
        }
        if(mSession.getSessionType() == 3){
            return new H264Stream(mediaExtractor,mSession);
        }
        return null;
    }
}
