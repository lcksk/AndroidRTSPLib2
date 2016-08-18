package com.example.ljd.mylibstreaming.LibRTSP.mp4;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Base64;
import android.util.Log;

import com.example.ljd.mylibstreaming.LibRTSP.quality.VideoQuality;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by ljd-pc on 2016/6/22.
 * 用于获取实时录制的视频流的 SPS 和 PPS
 * 将一个全0矩阵写到本地mp4文件中，获取SPS和PPS
 */
public class PpsSpsGetter {

    public final static String TAG = "PpsSpsGetter";
    private String mProfilLevel, mPPS, mSPS;
    private byte[] mPPSbyte,mSPSbyte;
    private int mScreenWidth;
    private int mScreenHeight;
    private static final String MIME_TYPE = "video/avc";
    MediaCodec.BufferInfo info;
    MediaFormat format;
    MediaCodec mediaCodec;
    private byte[] yuv420;
    private int framerate;

    public PpsSpsGetter(String fileName){
        getLocalFileSPSandPPS(fileName);
    }
    public PpsSpsGetter(VideoQuality mVideoQuality){
        mScreenWidth = mVideoQuality.getmWidth();
        mScreenHeight = mVideoQuality.getmHeight();
        framerate = mVideoQuality.getmFrameRate();
        GetPPSandSPS();
    }

    static String toHexString(byte[] buffer,int start, int len) {
        String c;
        StringBuilder s = new StringBuilder();
        for (int i=start;i<start+len;i++) {
            c = Integer.toHexString(buffer[i]&0xFF);
            s.append( c.length()<2 ? "0"+c : c );
        }
        return s.toString();
    }

    public String getProfileLevel() {
        return mProfilLevel;
    }

    public String getB64PPS() {
        Log.d(TAG, "PPS: "+mPPS);
        return mPPS;
    }

    public String getB64SPS() {
        Log.d(TAG, "SPS: "+mSPS);
        return mSPS;
    }

    private void GetPPSandSPS(){
        createTestImage();
        prepareEncoder();
        drainEncoder();
    }

    private void createTestImage() {
        int mSize = mScreenWidth*mScreenHeight;
        yuv420 = new byte[mSize*3/2];
        for (int i=0;i<mSize;i++) {
            yuv420[i] = (byte) (40+i%199);
        }
        for (int i=mSize;i<3*mSize/2;i+=2) {
            yuv420[i] = (byte) (40+i%200);
            yuv420[i+1] = (byte) (40+(i+99)%200);
        }

    }

    private void prepareEncoder(){
        info = new MediaCodec.BufferInfo();
        format = MediaFormat.createVideoFormat(MIME_TYPE,mScreenWidth,mScreenHeight);
        format.setInteger(MediaFormat.KEY_BIT_RATE, 125000);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, framerate);
        //COLOR_Format32bitARGB8888
        //COLOR_Format24bitRGB888
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);

        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, yuv420.length);
        try {
            mediaCodec = MediaCodec.createEncoderByType(MIME_TYPE);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mediaCodec.start();
        Log.d(TAG, "encoder output format not changed: " + format);
    }
    private void drainEncoder(){
        final int TIMEOUT_USEC = 10000;
        ByteBuffer[] inputBuffers = mediaCodec.getInputBuffers();
        ByteBuffer[] encoderOutputBuffers = mediaCodec.getOutputBuffers();
        while (true) {
            int inputBufferIndex = mediaCodec.dequeueInputBuffer(-1);
            int encoderStatus = mediaCodec.dequeueOutputBuffer(info, TIMEOUT_USEC);//dequeue出队，
            if (inputBufferIndex >= 0)
            {
                ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                inputBuffer.clear();
                inputBuffer.put(yuv420);
                mediaCodec.queueInputBuffer(inputBufferIndex, 0, yuv420.length, 0, 0);
            }
            if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                MediaFormat newFormat = mediaCodec.getOutputFormat();
                ByteBuffer spsb = newFormat.getByteBuffer("csd-0");
                ByteBuffer ppsb = newFormat.getByteBuffer("csd-1");
                mSPSbyte = new byte[spsb.capacity()-4];
                spsb.position(4);
                spsb.get(mSPSbyte,0,mSPSbyte.length);
                mPPSbyte = new byte[ppsb.capacity()-4];
                ppsb.position(4);
                ppsb.get(mPPSbyte,0,mPPSbyte.length);
                Log.e(TAG,"获取SPS和PPS成功！");
                break;      // out of while
            }else{
                Log.e(TAG,"获取SPS和PPS失败！");
            }
        }
        mPPS = Base64.encodeToString(mPPSbyte, 0, mPPSbyte.length, Base64.NO_WRAP);
        mSPS = Base64.encodeToString(mSPSbyte, 0, mSPSbyte.length, Base64.NO_WRAP);
        mProfilLevel = PpsSpsGetter.toHexString(Base64.decode(mSPS, Base64.NO_WRAP),1,3);
        releaseEncoder();
    }

    public void getLocalFileSPSandPPS(String fileName){
        ObtainSPSAndPPS obtainSPSAndPPS = new ObtainSPSAndPPS();
        try {
            obtainSPSAndPPS.getSPSAndPPS(fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mPPSbyte = obtainSPSAndPPS.getPPS();
        mSPSbyte = obtainSPSAndPPS.getSPS();
        mPPS = Base64.encodeToString(mPPSbyte, 0, mPPSbyte.length, Base64.NO_WRAP);
        mSPS = Base64.encodeToString(mSPSbyte, 0, mSPSbyte.length, Base64.NO_WRAP);
        mProfilLevel = PpsSpsGetter.toHexString(Base64.decode(mSPS, Base64.NO_WRAP),1,3);
    }

    private void releaseEncoder() {
        Log.d(TAG, "releasing encoder objects");
        if (mediaCodec != null) {
            mediaCodec.stop();
            mediaCodec.release();
            mediaCodec = null;
        }
    }

}
