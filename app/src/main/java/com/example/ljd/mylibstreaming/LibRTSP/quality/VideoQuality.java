package com.example.ljd.mylibstreaming.LibRTSP.quality;

/**
 * Created by ljd-pc on 2016/7/1.
 */
public class VideoQuality extends MediaQuality{
    private static final String TAG = "VideoQuality";

    private int mHeight;//视频的高度
    private int mWidth;//视频的宽度
    private int mFrameRate;//视频的帧率
    private int mBitRate;//视频的码率
    private int mDensity;//视频的密度

    public final static VideoQuality DEFAULT_VIDEO_QUALITY = new VideoQuality(176,144,20,500000);
    /**	Represents a quality for a video stream. */
    public VideoQuality() {}

    /**
     * Represents a quality for a video stream.
     * @param resX The horizontal resolution
     * @param resY The vertical resolution
     */
    public VideoQuality(int resX, int resY) {
        this.mWidth = resX;
        this.mHeight = resY;
    }

    /**
     * Represents a quality for a video stream.
     * @param resX The horizontal resolution
     * @param resY The vertical resolution
     * @param framerate The framerate in frame per seconds
     * @param bitrate The bitrate in bit per seconds
     */
    public VideoQuality(int resX, int resY, int framerate, int bitrate) {
        this.mFrameRate = framerate;
        this.mBitRate = bitrate;
        this.mWidth = resX;
        this.mHeight = resY;
    }
    public VideoQuality(int resX, int resY, int framerate, int bitrate,int resScreenDensity) {
        this.mFrameRate = framerate;
        this.mBitRate = bitrate;
        this.mWidth = resX;
        this.mHeight = resY;
        this.mDensity = resScreenDensity;
    }

    public VideoQuality clone() {
        return new VideoQuality(mWidth,mHeight,mFrameRate,mBitRate,mDensity);
    }


    public int getmHeight() {
        return mHeight;
    }

    public void setmHeight(int mHeight) {
        this.mHeight = mHeight;
    }

    public int getmFrameRate() {
        return mFrameRate;
    }

    public void setmFrameRate(int mFrameRate) {
        this.mFrameRate = mFrameRate;
    }

    public int getmBitRate() {
        return mBitRate;
    }

    public void setmBitRate(int mBitRate) {
        this.mBitRate = mBitRate;
    }

    public int getmWidth() {
        return mWidth;
    }

    public void setmWidth(int mWidth) {
        this.mWidth = mWidth;
    }

    public int getmDensity() {
        return mDensity;
    }

    public void setmDensity(int mDensity) {
        this.mDensity = mDensity;
    }
}
