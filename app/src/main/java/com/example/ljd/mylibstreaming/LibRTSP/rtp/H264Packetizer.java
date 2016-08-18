package com.example.ljd.mylibstreaming.LibRTSP.rtp;

import android.annotation.SuppressLint;
import android.util.Log;

import com.example.ljd.mylibstreaming.LibRTSP.session.Session;
import com.example.ljd.mylibstreaming.LibRTSP.utility.RunState;

import java.io.IOException;

/**
 * Created by ljd-pc on 2016/6/22.
 */
public class H264Packetizer extends AbstractPacketizer implements Runnable{
    private boolean VERBOSE = true;
    public final static String TAG = "H264Packetizer";

    private Thread t = null;
    private int naluLength = 0;
    private long delay = 0, oldtime = 0;
    private Statistics stats = new Statistics();
    private byte[] sps = null, pps = null, stapa = null;
    byte[] header = new byte[5];
    private int count = 0;
    private int streamType = 1;
    private Session session;


    public H264Packetizer(Session session) {
        super();
        this.session = session;
        socket.setClockFrequency(90000);
    }

    public void start() {
        if (t == null) {
            t = new Thread(this);
            t.start();
        }
    }

    public void stop() {
        if (t != null) {
            try {
                is.close();
            } catch (IOException e) {}
            t.interrupt();//interrupt()之后，再join()，或sleep()，就会抛出中断异常用来中断。
            //这和在sleep()时interrupt()的效果是一样的。
            try {
                t.join();//当 a thread 调用Join方法的时候，MainThread 就被停止执行，直到 a thread 线程执行完毕。
                //当子线程比较耗时的情况下，在子线程结束后，再结束主线程，就会用到join（）
            } catch (InterruptedException e) {}
            t = null;
        }
    }

    public void setStreamParameters(byte[] pps, byte[] sps) {
        this.pps = pps;
        this.sps = sps;

        // A STAP-A NAL (NAL type 24) containing the sps and pps of the stream
        if (pps != null && sps != null) {
            // STAP-A NAL header + NALU 1 (SPS) size + NALU 2 (PPS) size = 5 bytes
            stapa = new byte[sps.length + pps.length + 5];

            // STAP-A NAL header is 24
            stapa[0] = 24;

            // Write NALU 1 size into the array (NALU 1 is the SPS).
            stapa[1] = (byte) (sps.length >> 8);//高位
            stapa[2] = (byte) (sps.length & 0xFF);//低位

            // Write NALU 2 size into the array (NALU 2 is the PPS).
            stapa[sps.length + 3] = (byte) (pps.length >> 8);
            stapa[sps.length + 4] = (byte) (pps.length & 0xFF);

            // Write NALU 1 into the array, then write NALU 2 into the array.
            System.arraycopy(sps, 0, stapa, 3, sps.length);
            System.arraycopy(pps, 0, stapa, 5 + sps.length, pps.length);
        }
    }

    public void run() {
        long duration = 0;
        Log.d(TAG,"H264 packetizer started !");
        stats.reset();
        count = 0;
        socket.setCacheSize(0);
        try {
            while (!Thread.interrupted() && RunState.getInstance().isRun()) {
                oldtime = System.nanoTime();
                // We read a NAL units from the input stream and we send them

                send();
                // We measure how long it took to receive NAL units from the phone
                duration = System.nanoTime() - oldtime;

                stats.push(duration);
                // Computes the average duration of a NAL unit
                delay = stats.average();
                //Log.d(TAG,"duration: "+duration/1000000+" delay: "+delay/1000000);

            }
        } catch (IOException e) {
        } catch (InterruptedException e) {}

        Log.d(TAG,"H264 packetizer stopped !");

    }

    /**
     * Reads a NAL unit in the FIFO and sends it.
     * If it is too big, we split it in FU-A units (RFC 3984).
     */
    @SuppressLint("NewApi")
    private void send() throws IOException, InterruptedException {
        //Log.d(TAG,"H264 packetizer send() !");
        int sum = 1, len = 0, type;
        //if(session.getSessionType() == 1) {
            if (streamType == 0) {
                // NAL units are preceeded by their length, we parse the length
                fill(header, 0, 5);
                ts += delay;
                naluLength = header[3] & 0xFF | (header[2] & 0xFF) << 8 | (header[1] & 0xFF) << 16 | (header[0] & 0xFF) << 24;
                if (naluLength > 100000 || naluLength < 0) resync();
            } else if (streamType == 1) {//我们用的这个
                // if(VERBOSE) Log.v(TAG,"streamType == 1");
                // NAL units are preceeded with 0x00000001
                fill(header, 0, 5);//从inputstream装字节到header中
                //if(VERBOSE) Log.v(TAG,"header[4] = "+header[4]);
                if(session.getSessionType() == 1) {
                    ts = ((MediaCodecInputStream) is).getLastBufferInfo().presentationTimeUs * 1000L;
                }
                if(session.getSessionType() == 3){
                    ts = ((MediaExtractorInputStream) is).getLastBufferInfo().presentationTimeUs * 1000L;
                }
                //ts += delay;
                naluLength = is.available() + 1;
                if (!(header[0] == 0 && header[1] == 0 && header[2] == 0)) {
                    // Turns out, the NAL units are not preceeded with 0x00000001
                    Log.e(TAG, "NAL units are not preceeded by 0x00000001");
                    streamType = 2;
                    return;
                }
            } else {
                // Nothing preceededs the NAL units
                if (VERBOSE) Log.v(TAG, "Nothing preceededs the NAL units");
                fill(header, 0, 1);
                header[4] = header[0];
                ts = ((MediaCodecInputStream) is).getLastBufferInfo().presentationTimeUs * 1000L;
                //ts += delay;
                naluLength = is.available() + 1;
            }

            // Parses the NAL unit type
            //if(VERBOSE) Log.v(TAG,"Parses the NAL unit type");
            type = header[4] & 0x1F;
            if(VERBOSE) Log.v(TAG,"type = "+type);

            // The stream already contains NAL unit type 7 or 8, we don't need
            // to add them to the stream ourselves
            if (type == 7 || type == 8) {
                if (VERBOSE) Log.v(TAG, "SPS or PPS present in the stream.");
                count++;
                if (count > 4) {
                    sps = null;
                    pps = null;
                }
            }
            //如果这一帧是I帧，而sps != null && pps != null，
            //如果sps和pps已经发送，sps和pps会被置为Null,不为null，说明sps和pps还没有发送。
            //那就先发送sps和pps
            //type = 5,表示I帧
            // We send two packets containing NALU type 7 (SPS) and 8 (PPS)
            // Those should allow the H264 stream to be decoded even if no SDP was sent to the decoder.
            if (type == 5 && sps != null && pps != null) {
                if (VERBOSE)
                    Log.v(TAG, "We send two packets containing NALU type 7 (SPS) and 8 (PPS).");
                buffer = socket.requestBuffer();
                socket.markNextPacket();
                socket.updateTimestamp(ts);
                System.arraycopy(stapa, 0, buffer, rtphl, stapa.length);
                super.send(rtphl + stapa.length);
            }

            //Log.d(TAG,"- Nal unit length: " + naluLength + " delay: "+delay/1000000+" type: "+type);

            // Small NAL unit => Single NAL unit
            if (naluLength <= MAXPACKETSIZE - rtphl - 2) {
                //if(VERBOSE) Log.v(TAG,"naluLength<=MAXPACKETSIZE-rtphl-2");
                buffer = socket.requestBuffer();//获取一个空buffer
                buffer[rtphl] = header[4];
                len = fill(buffer, rtphl + 1, naluLength - 1);//向空buffer中填数据
                socket.updateTimestamp(ts);
                socket.markNextPacket();
                super.send(naluLength + rtphl);//发送buffer
                if(VERBOSE) Log.d(TAG,"----- Single NAL unit - len:"+len+" delay: "+delay);
            }
            // Large NAL unit => Split nal unit
            else {
                //if(VERBOSE) Log.v(TAG,"Large NAL unit => Split nal unit");
                // Set FU-A header
                header[1] = (byte) (header[4] & 0x1F);  // FU header type
                header[1] += 0x80; // Start bit
                // Set FU-A indicator
                header[0] = (byte) ((header[4] & 0x60) & 0xFF); // FU indicator NRI
                header[0] += 28;

                while (sum < naluLength) {
                    buffer = socket.requestBuffer();
                    buffer[rtphl] = header[0];
                    buffer[rtphl + 1] = header[1];
                    socket.updateTimestamp(ts);
                    if ((len = fill(buffer, rtphl + 2, naluLength - sum > MAXPACKETSIZE - rtphl - 2 ? MAXPACKETSIZE - rtphl - 2 : naluLength - sum)) < 0)
                        return;
                    sum += len;
                    // Last packet before next NAL
                    if (sum >= naluLength) {
                        // End bit on
                        buffer[rtphl + 1] += 0x40;
                        socket.markNextPacket();
                    }
                    super.send(len + rtphl + 2);
                    // Switch start bit
                    header[1] = (byte) (header[1] & 0x7F);
                    //Log.d(TAG,"----- FU-A unit, sum:"+sum);
                }
            }
        //}
//        if (session.getSessionType() == 3){
//
//        }
    }
    //获取codec数据到buffer
    private int fill(byte[] buffer, int offset,int length) throws IOException {
        //if(VERBOSE) Log.v(TAG,"获取codec数据到buffer");
        int sum = 0, len;
        while (sum<length) {
            //if(VERBOSE) Log.v(TAG,"获取codec数据到buffer");
            len = is.read(buffer, offset+sum, length-sum);
            //if(VERBOSE) Log.v(TAG,"获取codec数据到buffer");
            if (len<0) {
                throw new IOException("End of stream");
            }
            else sum+=len;
        }
        //if(VERBOSE) Log.v(TAG,"获取codec数据到buffer");
        return sum;
    }

    private void resync() throws IOException {
        int type;

        Log.e(TAG,"Packetizer out of sync ! Let's try to fix that...(NAL length: "+naluLength+")");

        while (true) {

            header[0] = header[1];
            header[1] = header[2];
            header[2] = header[3];
            header[3] = header[4];
            header[4] = (byte) is.read();

            type = header[4]&0x1F;

            if (type == 5 || type == 1) {
                naluLength = header[3]&0xFF | (header[2]&0xFF)<<8 | (header[1]&0xFF)<<16 | (header[0]&0xFF)<<24;
                if (naluLength>0 && naluLength<100000) {
                    oldtime = System.nanoTime();
                    Log.e(TAG,"A NAL unit may have been found in the bit stream !");
                    break;
                }
                if (naluLength==0) {
                    Log.e(TAG,"NAL unit with NULL size found...");
                } else if (header[3]==0xFF && header[2]==0xFF && header[1]==0xFF && header[0]==0xFF) {
                    Log.e(TAG,"NAL unit with 0xFFFFFFFF size found...");
                }
            }

        }

    }

}
