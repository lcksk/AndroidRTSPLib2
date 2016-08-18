package com.example.ljd.mylibstreaming.LibRTSP.encorder.video;

import android.media.MediaExtractor;
import android.media.MediaFormat;

import com.example.ljd.mylibstreaming.LibRTSP.session.Session;

import java.io.IOException;

/**
 * Created by ljd-pc on 2016/7/12.
 */
public class MP4Encorder extends VideoEncorder {

    private MediaExtractor mediaExtractor;
    private int framerate;
    public MP4Encorder(Session session){
        mediaExtractor = new MediaExtractor();
        try {
            mediaExtractor.setDataSource(session.getVideoPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
        for(int i = 0; i < mediaExtractor.getTrackCount(); i++) {
            MediaFormat format = mediaExtractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (!mime.startsWith("video/")) {
                continue;
            }
            framerate = format.getInteger(MediaFormat.KEY_FRAME_RATE);
            mediaExtractor.selectTrack(i);
            session.getVideoQuality().setmFrameRate(framerate);
        }
    }

    public void stop(){
        mediaExtractor.release();
    }

    public MediaExtractor getMediaEncorder3(){
        return mediaExtractor;
    }
}
