package com.example.ljd.mylibstreaming.LibRTSP.encorder.video;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.util.Log;
import android.view.Surface;

import com.example.ljd.mylibstreaming.LibRTSP.quality.VideoQuality;
import com.example.ljd.mylibstreaming.LibRTSP.session.Session;

import java.io.IOException;

/**
 * Created by ljd-pc on 2016/7/13.
 */
public class CameraEncorder extends VideoEncorder {
    private static final String TAG = "CameraEncorder";
    private VideoQuality mRequestedQuality;
    private MediaCodec mMediaCodec;
    private String mMimeType = "video/avc";
    private Session session;



    private Surface surface;

    public CameraEncorder(Session session){
        this.session = session;
        this.mRequestedQuality = session.getVideoQuality();
    }

    public void encodeWithMediaCodec() throws RuntimeException {

        Log.d(TAG, "Video encoded using the MediaCodec API with a surface");
        Log.v(TAG, "encodeWithMediaCodec() 的各种信息长，宽，屏幕密度：" + mRequestedQuality.getmWidth() + " " + mRequestedQuality.getmHeight() + " " + mRequestedQuality.getmDensity());
        MediaFormat mediaFormat = MediaFormat.createVideoFormat(mMimeType, mRequestedQuality.getmWidth(), mRequestedQuality.getmHeight());
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, mRequestedQuality.getmBitRate());
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, mRequestedQuality.getmFrameRate());
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
        try {
            mMediaCodec = MediaCodec.createEncoderByType(mMimeType);//创建了一个MediaCodec
        } catch (IOException e) {
            e.printStackTrace();
        }
        mMediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        surface = mMediaCodec.createInputSurface();
        mMediaCodec.start();
    }

    public Surface getSurface() {
        return surface;
    }

    public void setSurface(Surface surface) {
        this.surface = surface;
    }

    public void stop(){
        mMediaCodec.stop();
        mMediaCodec.release();
        mMediaCodec = null;
    }

    public MediaCodec getMediaEncorder1(){
        return mMediaCodec;
    }

}
