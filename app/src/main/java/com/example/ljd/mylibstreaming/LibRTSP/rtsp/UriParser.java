package com.example.ljd.mylibstreaming.LibRTSP.rtsp;

/**
 * Created by ljd-pc on 2016/7/7.
 */

import android.content.ContentValues;
import android.hardware.Camera;
import android.util.Log;

import com.example.ljd.mylibstreaming.LibRTSP.quality.VideoQuality;
import com.example.ljd.mylibstreaming.LibRTSP.session.AbstractSessionFactory;
import com.example.ljd.mylibstreaming.LibRTSP.session.Session;
import com.example.ljd.mylibstreaming.LibRTSP.session.VideoSessionFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;


/**
 * This class parses URIs received by the RTSP server and configures a Session accordingly.
 */
public class UriParser {

    public final static String TAG = "UriParser";

    /**
     * Configures a Session according to the given URI.
     * Here are some examples of URIs that can be used to configure a Session:
     * <ul><li>rtsp://xxx.xxx.xxx.xxx:8086?h264&flash=on</li>
     * <li>rtsp://xxx.xxx.xxx.xxx:8086?h263&camera=front&flash=on</li>
     * <li>rtsp://xxx.xxx.xxx.xxx:8086?h264=200-20-320-240</li>
     * <li>rtsp://xxx.xxx.xxx.xxx:8086?aac</li></ul>
     * @param uri The URI
     * @throws IllegalStateException
     * @throws IOException
     * @return A Session configured according to the URI
     */

    /*
     *应该根据URL选择合适的Session工厂来新建Session
     */
    public static Session parse(String uri,Session sessionForClone) throws IllegalStateException, IOException {
        //当解析得到的结果，要求产生一个视频session时。
        int sessionType = 1;
        VideoQuality videoQuality = sessionForClone.getScreenVideoQuality();
        if(uri.contains("screen")){
            sessionType = 1;
            videoQuality = sessionForClone.getScreenVideoQuality();
        }else if(uri.contains("camera")){
            sessionType = 2;
            videoQuality = sessionForClone.getVideoQuality();
        }else if(uri.contains("movie")){
            sessionType = 3;
            videoQuality = sessionForClone.getFileVideoQulity();
        }else{

        }
        AbstractSessionFactory mSessionFactory = VideoSessionFactory.getInstance();
        Session mSession = mSessionFactory.CreatSession(sessionType,sessionForClone.getVideoPath(),sessionForClone.getDestination(),sessionForClone.getDestinationPort()
                ,videoQuality,sessionForClone.getTimeToLive(),
                sessionForClone.getOrigin(),sessionForClone.getOriginPort(),sessionForClone.getMediaProjection());
        return mSession;
    }

}
