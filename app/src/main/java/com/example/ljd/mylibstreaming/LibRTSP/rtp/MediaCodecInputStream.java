package com.example.ljd.mylibstreaming.LibRTSP.rtp;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;

import com.example.ljd.mylibstreaming.LibRTSP.utility.RunState;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * Created by ljd-pc on 2016/6/22.
 * 装饰者模式
 * 继承 InputStream
 * 重写 InputStream 的 read 方法
 */
public class MediaCodecInputStream extends InputStream{

    private static final String TAG = "MediaCodecInputStream";
    private boolean VERBOSE = false;

    private MediaCodec mMediaCodec;
    private MediaCodec.BufferInfo mBufferInfo;
    private MediaFormat mMediaFormat;
    private ByteBuffer[] mBuffers;
    private ByteBuffer mBuffer;
    private int mIndex = -1;


    public MediaCodecInputStream(MediaCodec mediaCodec){
        this.mMediaCodec = mediaCodec;
        this.mBuffers = mMediaCodec.getOutputBuffers();
        this.mBufferInfo = new MediaCodec.BufferInfo();
    }

    public int read() {return 0;}
    //把 MediaCodec 输出缓存中的数据写入到 buffer
    public int read(byte[] buffer, int offset, int length) throws IOException{
        //线程没有被中断，并且程序正在运行
        if(VERBOSE) Log.v(TAG,"read");
        int min = 0;
        try {
            if (mBuffer == null) {//这个很有用
                while (!Thread.interrupted()) {
                    mIndex = mMediaCodec.dequeueOutputBuffer(mBufferInfo, 500000);
                    if (mIndex >= 0) {
                        //if(VERBOSE) Log.v(TAG,"mIndex > 0");
                        if (VERBOSE) Log.v(TAG, "Index: " + mIndex + " Time: " + mBufferInfo.presentationTimeUs + " size: " + mBufferInfo.size);
                        mBuffer = mBuffers[mIndex];
                        mBuffer.position(0);
                        break;
                    } else if (mIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                        if (VERBOSE) Log.v(TAG, "mIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED");
                        mBuffers = mMediaCodec.getOutputBuffers();
                    } else if (mIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        if (VERBOSE) Log.v(TAG, "mIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED");
                        mMediaFormat = mMediaCodec.getOutputFormat();
                    } else if (mIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        //No buffer available...
                        //发生原因 time out
                        if (VERBOSE) Log.v(TAG, "mIndex == MediaCodec.INFO_TRY_AGAIN_LATER");
                    } else {
                        if (VERBOSE) Log.e(TAG, "read() mIndex: " + mIndex);
                    }
                }
            }
            min = (length < mBufferInfo.size - mBuffer.position()) ? length : mBufferInfo.size - mBuffer.position();
            mBuffer.get(buffer, offset, min);//mBuffer 自己复制一份，给 buffer赋值
            if(VERBOSE) Log.v(TAG,"mBuffer.get(buffer, offset, min);");
            //释放mBuffers
            if (mBuffer.position() >= mBufferInfo.size) {
                mMediaCodec.releaseOutputBuffer(mIndex, false);
                mBuffer = null;
                if(VERBOSE) Log.v(TAG,"releaseOutputBuffer");
           }
        }catch (RuntimeException e){
            e.printStackTrace();
        }

        return min;
    }

    public int available() {
        if (mBuffer != null)
            return mBufferInfo.size - mBuffer.position();
        else
            return 0;
    }

    public MediaCodec.BufferInfo getLastBufferInfo() {
        return mBufferInfo;
    }
}
