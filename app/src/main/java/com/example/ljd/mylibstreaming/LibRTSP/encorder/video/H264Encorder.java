package com.example.ljd.mylibstreaming.LibRTSP.encorder.video;

import android.hardware.display.DisplayManager;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.util.Log;
import android.view.Surface;

import com.example.ljd.mylibstreaming.LibRTSP.quality.VideoQuality;

import java.io.IOException;

/**
 * Created by ljd-pc on 2016/7/3.
 * 一个策略模式
 * 用来实现功能：
 *  ·准备编码器
 *  ·创建virtualDisplay
 */
public class H264Encorder extends VideoEncorder {

    private static final String TAG = "H264Encorder";
    private VideoQuality mRequestedQuality;
    private MediaCodec mMediaCodec;
    private MediaProjection mMediaProjection;
    private String mMimeType = "video/avc";


    public H264Encorder( VideoQuality mRequestedQuality, MediaProjection mMediaProjection ) {
        this.mRequestedQuality = mRequestedQuality;
        this.mMediaProjection = mMediaProjection;
    }

    @Override
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
        Surface surface = mMediaCodec.createInputSurface();
        virtualDisplay(surface);
        mMediaCodec.start();
    }

    private void virtualDisplay(Surface mSurface) {
        Log.v(TAG, "private void virtualDisplay: create virtual displayed");
        Log.v(TAG, "virtualDisplay 的各种信息长，宽，屏幕密度：" + mRequestedQuality.getmWidth() + " " + mRequestedQuality.getmHeight() + " " + mRequestedQuality.getmDensity());
        if(mMediaProjection == null){
            Log.e(TAG, "mMediaProjection == null");
        }

        mMediaProjection.createVirtualDisplay("屏幕捕捉", mRequestedQuality.getmWidth(), mRequestedQuality.getmHeight(),
                mRequestedQuality.getmDensity(), DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mSurface, null, null);

    }

    public void stop(){
        if(mMediaCodec!=null){
            mMediaCodec.stop();
            mMediaCodec.release();
            mMediaCodec = null;
        }

    }

    public MediaCodec getMediaEncorder1(){
        return mMediaCodec;
    }
}
