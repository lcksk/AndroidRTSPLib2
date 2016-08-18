package com.example.ljd.mylibstreaming.LibRTSP.rtp;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;

import com.example.ljd.mylibstreaming.LibRTSP.session.Session;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * Created by ljd-pc on 2016/7/12.
 */
public class MediaExtractorInputStream extends InputStream{

    private String TAG = "MediaExtractorInputStream";
    private boolean VERBOSE = true;
    private MediaExtractor mediaExtractor;
    private ByteBuffer mBuffer = null;
    private MediaCodec.BufferInfo info;
    private int mVideoTrackIndex = -1;
    private int framerate = 30;
    private int length = 0;
    private int min = 0;

    public MediaExtractorInputStream(MediaExtractor mediaExtractor){
        this.mediaExtractor = mediaExtractor;
        info = new MediaCodec.BufferInfo();
        info.presentationTimeUs = 0;

    }
    @Override
    public int read() throws IOException {
        return 0;
    }

    public int read(byte[] buffer, int offset, int length){
        if(mBuffer == null){
            while(!Thread.interrupted()){
                mBuffer = ByteBuffer.allocate(1024*100);
                int sampleSize = mediaExtractor.readSampleData(mBuffer, 0);
                if (VERBOSE)Log.v(TAG,"sampleSize = " + sampleSize);
                if(sampleSize < 0) {
                    break;
                }

                if(sampleSize >= 0){
                    mediaExtractor.advance();
                    info.offset = 0;
                    info.size = sampleSize;
                    info.flags = MediaCodec.BUFFER_FLAG_SYNC_FRAME;
                    info.presentationTimeUs += 1000*1000/framerate;
                    mBuffer.position(0);
                    break;
                }
            }
        }
        min = (length < info.size - mBuffer.position()) ? length : info.size - mBuffer.position();
        mBuffer.get(buffer, offset, min);//mBuffer 自己复制一份，给 buffer赋值
        //if(VERBOSE) Log.v(TAG,"mBuffer.position() = "+mBuffer.position());
        if (mBuffer.position() >= info.size) {
            mBuffer = null;
        }
        return min;
    }
    public int available() {
        if (mBuffer != null)
            return info.size - mBuffer.position();
        else
            return 0;
    }

    public MediaCodec.BufferInfo getLastBufferInfo() {
        return info;
    }

}
