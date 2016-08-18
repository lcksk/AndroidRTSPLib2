package com.example.ljd.mylibstreaming.LibRTSP.stream;

import android.media.MediaCodec;
import android.media.MediaExtractor;

import com.example.ljd.mylibstreaming.LibRTSP.session.Session;

/**
 * Created by ljd-pc on 2016/7/4.
 */
abstract public class AbstractStreamFactory {
    abstract public MediaStream CreateStream(MediaCodec mMediaCodec, MediaExtractor mediaExtractor,Session mSession);
}
