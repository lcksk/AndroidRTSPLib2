package com.example.ljd.mylibstreaming.LibRTSP.stream;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.util.Log;

import com.example.ljd.mylibstreaming.LibRTSP.rtp.AbstractPacketizer;
import com.example.ljd.mylibstreaming.LibRTSP.session.Session;
import com.example.ljd.mylibstreaming.LibRTSP.stream.video.VideoStream;

import java.io.IOException;
import java.net.InetAddress;

/**
 * Created by ljd-pc on 2016/6/22.
 * abstract 类可以不实现接口中的方法
 */
public abstract class MediaStream implements Stream {
    private boolean VERBOSE = true;
    protected static final String TAG = "MediaStream";

    /** The packetizer that will read the output of the camera and send RTP packets over the networked. */
    protected AbstractPacketizer mPacketizer = null;
    protected boolean mStreaming = false, mConfigured = false;
    protected int mRtpPort = 0, mRtcpPort = 0;
    protected InetAddress mDestination;
    private int mTTL = 64;
    protected MediaCodec mMediaCodec;
    protected MediaExtractor mediaExtractor;
    protected Session mSession;

    public MediaStream(Session session) {
        this.mSession = session;
        this.mDestination = session.getDestinationAddress();
        setDestinationPorts(session.getDestinationPort());
        if(VERBOSE) Log.v(TAG,"session.getDestinationPort() IS "+session.getDestinationPort());
        if(VERBOSE) Log.v(TAG,"session.getDestinationAddress() IS "+session.getDestinationAddress());
        mTTL = session.getTimeToLive();
    }

    /**
     * Sets the destination IP address of the stream.
     * @param dest The destination address of the stream
     */
    public void setDestinationAddress(InetAddress dest) {
        mDestination = dest;
    }

    /**
     * Sets the destination ports of the stream.
     * If an odd number is supplied for the destination port then the next
     * lower even number will be used for RTP and it will be used for RTCP.
     * If an even number is supplied, it will be used for RTP and the next odd
     * number will be used for RTCP.
     * @param dport The destination port
     */
    public void setDestinationPorts(int dport) {
        if (dport % 2 == 1) {
            mRtpPort = dport-1;
            mRtcpPort = dport;
        } else {
            mRtpPort = dport;
            mRtcpPort = dport+1;
        }
    }

    /**
     * Sets the destination ports of the stream.
     * @param rtpPort Destination port that will be used for RTP
     * @param rtcpPort Destination port that will be used for RTCP
     */
    public void setDestinationPorts(int rtpPort, int rtcpPort) {
        mRtpPort = rtpPort;
        mRtcpPort = rtcpPort;
    }

    /**
     * Sets the Time To Live of packets sent over the network.
     * @param ttl The time to live
     * @throws IOException
     */
    public void setTimeToLive(int ttl) throws IOException {
        mTTL = ttl;
    }

    /**
     * Returns a pair of destination ports, the first one is the
     * one used for RTP and the second one is used for RTCP.
     **/
    public int[] getDestinationPorts() {
        return new int[] {
                mRtpPort,
                mRtcpPort
        };
    }

    /**
     * Returns a pair of source ports, the first one is the
     * one used for RTP and the second one is used for RTCP.
     **/
    public int[] getLocalPorts() {
        return mPacketizer.getRtpSocket().getLocalPorts();
    }

    /**
     * Returns the packetizer associated with the {@link MediaStream}.
     * @return The packetizer
     */
    public AbstractPacketizer getPacketizer() {
        return mPacketizer;
    }

    /**
     * Returns an approximation of the bit rate consumed by the stream in bit per seconde.
     */
    public long getBitrate() {
        return !mStreaming ? 0 : mPacketizer.getRtpSocket().getBitrate();
    }

    /**
     * Indicates if the {@link MediaStream} is streaming.
     * @return A boolean indicating if the {@link MediaStream} is streaming
     */
    public boolean isStreaming() {
        return mStreaming;
    }

    /**
     * Configures the stream with the settings supplied with
     */
    public synchronized void configure(Session session) throws IllegalStateException, IOException {
        if (mStreaming) throw new IllegalStateException("Can't be called while streaming.");
        if(VERBOSE) Log.v(TAG,"synchronized void configure()");
        if (mPacketizer != null) {
            if(VERBOSE) Log.v(TAG,"dest = "+ mDestination);
            if(VERBOSE) Log.v(TAG,"rtpPort = "+ mRtpPort);
            if(VERBOSE) Log.v(TAG,"rtcpPort = "+ mRtcpPort);
            mPacketizer.setDestination(mDestination, mRtpPort, mRtcpPort);
            mPacketizer.setTimeToLive(mTTL);
        }

        mConfigured = true;
    }

    /** Starts the stream. */
    public synchronized void start() throws IllegalStateException, IOException {
        if (mDestination==null)
            throw new IllegalStateException("No destination ip address set for the stream !");

        if (mRtpPort<=0 || mRtcpPort<=0)
            throw new IllegalStateException("No destination ports set for the stream !");
    }

    /** Stops the stream. */
    public synchronized  void stop() {
        if (mStreaming) {
            try {
                mPacketizer.stop();

            } catch (Exception e) {
                e.printStackTrace();
            }
            mStreaming = false;
        }
    }


    /**
     * Returns a description of the stream using SDP.
     * This method can only be called after {@link Stream#configure()}.
     * @throws IllegalStateException Thrown when {@link Stream#configure()} was not called.
     */
    public abstract String getSessionDescription();

    /**
     *
     * @return the SSRC of the stream
     */
    public int getSSRC() {
        return getPacketizer().getSSRC();
    }

}
