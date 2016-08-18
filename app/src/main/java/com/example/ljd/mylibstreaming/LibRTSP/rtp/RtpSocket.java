package com.example.ljd.mylibstreaming.LibRTSP.rtp;

import android.app.IntentService;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import com.example.ljd.mylibstreaming.LibRTSP.rtcp.SenderReporter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Created by ljd-pc on 2016/6/22.
 */
public class RtpSocket implements Runnable{
    public static final String TAG = "RtpSocket";
    private boolean VERBOSE = true;
    public static final int RTP_HEADER_LENGTH = 12;
    public static final int MTU = 1300;

    private MulticastSocket mSocket;
    private DatagramPacket[] mPackets;
    private byte[][] mBuffers;
    private long[] mTimestamps;

    private SenderReporter mReport;

    private Semaphore mBufferRequested, mBufferCommitted;
    private Thread mThread;

    private int mTransport;
    private long mCacheSize;
    private long mClock = 0;
    private long mOldTimestamp = 0;
    private int mSsrc, mSeq = 0, mPort = -1;
    private int mBufferCount, mBufferIn, mBufferOut;
    private int mCount = 0;
    private InetAddress myDest;
    private AverageBitrate mAverageBitrate;

    /**
     * This RTP socket implements a buffering mechanism relying on a FIFO of buffers and a Thread.
     * @throws IOException
     */
    public RtpSocket() {

        mCacheSize = 0;
        mBufferCount = 300; // TODO: readjust that when the FIFO is full
        mBuffers = new byte[mBufferCount][];
        mPackets = new DatagramPacket[mBufferCount];
        mReport = new SenderReporter();
        mAverageBitrate = new AverageBitrate();

        resetFifo();

        for (int i=0; i<mBufferCount; i++) {

            mBuffers[i] = new byte[MTU];
            mPackets[i] = new DatagramPacket(mBuffers[i], 1);

			/*							     Version(2)  Padding(0)					 					*/
			/*									 ^		  ^			Extension(0)						*/
			/*									 |		  |				^								*/
			/*									 | --------				|								*/
			/*									 | |---------------------								*/
			/*									 | ||  -----------------------> Source Identifier(0)	*/
			/*									 | ||  |												*/
            mBuffers[i][0] = (byte) Integer.parseInt("10000000",2);

			/* Payload Type */
            mBuffers[i][1] = (byte) 96;

			/* Byte 2,3        ->  Sequence Number                   */
			/* Byte 4,5,6,7    ->  Timestamp                         */
			/* Byte 8,9,10,11  ->  Sync Source Identifier            */

        }

        try {
            mSocket = new MulticastSocket();

        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }

    }

    private void resetFifo() {
        mCount = 0;
        mBufferIn = 0;
        mBufferOut = 0;
        mTimestamps = new long[mBufferCount];
        mBufferRequested = new Semaphore(mBufferCount);
        mBufferCommitted = new Semaphore(0);
        mReport.reset();
        mAverageBitrate.reset();
    }

    /** Closes the underlying socket. */
    public void close() {
        mSocket.close();
    }

    /** Sets the SSRC of the stream. */
    public void setSSRC(int ssrc) {
        this.mSsrc = ssrc;
        for (int i=0;i<mBufferCount;i++) {
            setLong(mBuffers[i], ssrc,8,12);
        }
        mReport.setSSRC(mSsrc);
    }

    /** Returns the SSRC of the stream. */
    public int getSSRC() {
        return mSsrc;
    }

    /** Sets the clock frequency of the stream in Hz. */
    public void setClockFrequency(long clock) {
        mClock = clock;
    }

    /** Sets the size of the FIFO in ms. */
    public void setCacheSize(long cacheSize) {
        mCacheSize = cacheSize;
    }

    /** Sets the Time To Live of the UDP packets. */
    public void setTimeToLive(int ttl) throws IOException {
        mSocket.setTimeToLive(ttl);
    }

    /** Sets the destination address and to which the packets will be sent. */
    public void setDestination(InetAddress dest, int dport, int rtcpPort) {
        if (dport != 0 && rtcpPort != 0) {
            myDest = dest;
            mPort = dport;
            if(VERBOSE) Log.v(TAG,"myDest IS "+myDest);
            if(VERBOSE) Log.v(TAG,"mPort IS "+mPort);
            for (int i=0;i<mBufferCount;i++) {
                mPackets[i].setPort(dport);
                mPackets[i].setAddress(dest);
            }
            mReport.setDestination(dest, rtcpPort);
        }
    }

    public int getPort() {
        return mPort;
    }

    public int[] getLocalPorts() {
        return new int[] {
                mSocket.getLocalPort(),
                mReport.getLocalPort()
        };

    }

    /**
     * Returns an available buffer from the FIFO, it can then be modified.
     * Call {@link #commitBuffer(int)} to send it over the network.
     * @throws InterruptedException
     **/
    public byte[] requestBuffer() throws InterruptedException {
        mBufferRequested.acquire();
        mBuffers[mBufferIn][1] &= 0x7F;
        return mBuffers[mBufferIn];
    }
//没用
    /** Puts the buffer back into the FIFO without sending the packet. */
    public void commitBuffer() throws IOException {

        if (mThread == null) {
            mThread = new Thread(this);
            mThread.start();//sends the packets in the FIFO one by one
        }

        if (++mBufferIn>=mBufferCount) mBufferIn = 0;
        mBufferCommitted.release();

    }

    /** Sends the RTP packet over the network. */
    public void commitBuffer(int length) throws IOException {
        updateSequence();
        mPackets[mBufferIn].setLength(length);

        mAverageBitrate.push(length);

        if (++mBufferIn>=mBufferCount) mBufferIn = 0;
        mBufferCommitted.release();

        if (mThread == null) {
            mThread = new Thread(this);
            mThread.start();//sends the packets in the FIFO one by one
        }

    }

    /** Returns an approximation of the bitrate of the RTP stream in bits per second. */
    public long getBitrate() {
        return mAverageBitrate.average();
    }

    /** Increments the sequence number. */
    private void updateSequence() {
        setLong(mBuffers[mBufferIn], ++mSeq, 2, 4);
    }

    /**
     * Overwrites the timestamp in the packet.
     * @param timestamp The new timestamp in ns.
     **/
    public void updateTimestamp(long timestamp) {
        mTimestamps[mBufferIn] = timestamp;
        setLong(mBuffers[mBufferIn], (timestamp/100L)*(mClock/1000L)/10000L, 4, 8);
    }

    /** Sets the marker in the RTP packet. */
    public void markNextPacket() {
        mBuffers[mBufferIn][1] |= 0x80;
    }

    /** The Thread sends the packets in the FIFO one by one at a constant rate. */
    @Override
    public void run() {
        Statistics stats = new Statistics(50,3000);
        try {
            // Caches mCacheSize milliseconds of the stream in the FIFO.
            Thread.sleep(mCacheSize);
            long delta = 0;
            while (mBufferCommitted.tryAcquire(4,TimeUnit.SECONDS)) {
                if (mOldTimestamp != 0) {
                    // We use our knowledge of the clock rate of the stream and the difference between two timestamps to
                    // compute the time lapse that the packet represents.
                    if ((mTimestamps[mBufferOut]-mOldTimestamp)>0) {
                        stats.push(mTimestamps[mBufferOut]-mOldTimestamp);
                        long d = stats.average()/1000000;
                        //Log.d(TAG,"delay: "+d+" d: "+(mTimestamps[mBufferOut]-mOldTimestamp)/1000000);
                        // We ensure that packets are sent at a constant and suitable rate no matter how the RtpSocket is used.
                        //由于采用的生产者/消费者模式。导致视频采样时间不能无限大，而是与网络发送时间相匹配。
                        //时间戳即为首字节的采样时间，通过时间戳可以计算出采样的时间间隔。
                        //然后通过这个时间间隔，设定为网络发送的时间间隔。
                        //if (mCacheSize>0)
                        Thread.sleep(d);//拥塞控制
                    } else if ((mTimestamps[mBufferOut]-mOldTimestamp)<0) {
                        Log.e(TAG, "TS: "+mTimestamps[mBufferOut]+" OLD: "+mOldTimestamp);
                    }
                    delta += mTimestamps[mBufferOut]-mOldTimestamp;
                    if (delta>500000000 || delta<0) {
                        //Log.d(TAG,"permits: "+mBufferCommitted.availablePermits());
                        delta = 0;
                    }
                }
                mReport.update(mPackets[mBufferOut].getLength(), (mTimestamps[mBufferOut]/100L)*(mClock/1000L)/10000L);
                mOldTimestamp = mTimestamps[mBufferOut];
                if (mCount++>30) {
                    //Log.v(TAG,"mSocket.send(mPackets[mBufferOut]);");
                    mSocket.send(mPackets[mBufferOut]);
                }
                if (++mBufferOut>=mBufferCount) mBufferOut = 0;
                mBufferRequested.release();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        mThread = null;
        resetFifo();
    }


    private void setLong(byte[] buffer, long n, int begin, int end) {
        for (end--; end >= begin; end--) {
            buffer[end] = (byte) (n % 256);
            n >>= 8;
        }
    }

    /**
     * Computes an average bit rate.
     * 计算网络拥塞情况，调整发送端帧率、码率
     **/
    protected static class AverageBitrate {

        private final static long RESOLUTION = 200;

        private long mOldNow, mNow, mDelta;
        private long[] mElapsed, mSum;//（时间）elapsed消逝，过去
        private int mCount, mIndex, mTotal;
        private int mSize;

        public AverageBitrate() {
            mSize = 5000/((int)RESOLUTION);
            reset();
        }

        public AverageBitrate(int delay) {
            mSize = delay/((int)RESOLUTION);
            reset();
        }

        public void reset() {
            mSum = new long[mSize];
            mElapsed = new long[mSize];
            mNow = SystemClock.elapsedRealtime();
            mOldNow = mNow;
            mCount = 0;
            mDelta = 0;
            mTotal = 0;
            mIndex = 0;
        }

        public void push(int length) {
            mNow = SystemClock.elapsedRealtime();
            if (mCount>0) {
                mDelta += mNow - mOldNow;
                mTotal += length;
                if (mDelta>RESOLUTION) {
                    mSum[mIndex] = mTotal;
                    mTotal = 0;
                    mElapsed[mIndex] = mDelta;
                    mDelta = 0;
                    mIndex++;
                    if (mIndex>=mSize) mIndex = 0;
                }
            }
            mOldNow = mNow;
            mCount++;
        }

        public int average() {
            long delta = 0, sum = 0;
            for (int i=0;i<mSize;i++) {
                sum += mSum[i];
                delta += mElapsed[i];
            }
            //Log.d(TAG, "Time elapsed: "+delta);
            return (int) (delta>0?8000*sum/delta:0);
        }

    }
    //拥塞控制
    /** Computes the proper rate at which packets are sent. */
    protected static class Statistics {

        public final static String TAG = "Statistics";

        private int count=500, c = 0;
        private float m = 0, q = 0;
        private long elapsed = 0;
        private long start = 0;
        private long duration = 0;
        private long period = 6000000000L;
        private boolean initoffset = false;

        public Statistics(int count, long period) {
            this.count = count;
            this.period = period*1000000L;
        }

        public void push(long value) {
            duration += value;
            elapsed += value;
            if (elapsed>period) {
                elapsed = 0;
                long now = System.nanoTime();
                if (!initoffset || (now - start < 0)) {
                    start = now;
                    duration = 0;
                    initoffset = true;
                }
                value -= (now - start) - duration;
                //Log.d(TAG, "sum1: "+duration/1000000+" sum2: "+(now-start)/1000000+" drift: "+((now-start)-duration)/1000000+" v: "+value/1000000);
            }
            if (c<40) {
                // We ignore the first 40 measured values because they may not be accurate
                c++;
                m = value;
            } else {
                m = (m*q+value)/(q+1);//m 每个包发送的平均时间，q之前发送的总包数
                if (q<count) q++;
            }
        }

        public long average() {
            long l = (long)m-2000000;
            return l>0 ? l : 0;
        }

    }


}
