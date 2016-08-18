
一、这是一个基于Android平台的RTSP直播、点播服务器。
    需要Android版本5.0以上。低于5.0的Android设备不能正常使用。
    在 Google Nexus 9 上测试，运行正常。
    
    完善了AndroidRTSPLib的功能，修复了AndroidRTSPLib的若干bug。
    
    AndroidRTSPLib不再维护，以后只维护AndroidRTSPLib2.

二、实现三个功能：

    1、将屏幕图像实时推流
    2、将摄像头图像实时推流
    3、将本地mp4、3gp格式的视频文件推流。

三、使用方式

    1、客户端可以使用VLC等播放器播放视频流。
    2、播放器输入的链接格式为 rtsp://IP地址:端口号/选项
    3、选项包括：screen（实时获取Android手机、平板屏幕图像）
              camera（实时获取Android手机、平板摄像头图像）
              movie（点播Android手机、平板本地视频文件）
                    文件路径为SD卡根目录下的"/AndroidRTSPLib/movie.mp4",
                    需要将视频文件名称更改为movie.mp4。
                    同时也支持.3gp文件，通过修改源码“MainActivity.java”中的 “VIDEO_PATH = SDCARD_PATH+"/AndroidRTSPLib/movie.mp4";”实现。
              
    4、连接示例：rtsp://192.168.1.20:1234/screen   （实时获取Android手机、平板屏幕图像）
              rtsp://192.168.1.20:1234/camera   （实时获取Android手机、平板摄像头图像）
              rtsp://192.168.1.20:1234/movie    （点播Android手机、平板本地视频文件）
              rtsp://192.168.1.20:1234          （默认情况下，实时获取Android手机、平板屏幕图像）

四、程序框图

（1）程序结构图

![image](https://github.com/lijundacom/AndroidRTSPLib/blob/master/程序结构图.png)

（2）运行流程图

![image](https://github.com/lijundacom/AndroidRTSPLib/blob/master/程序运行图.png)


五、程序效果图

(1)屏幕录制效果

![image](https://github.com/lijundacom/AndroidRTSPLib/blob/master/screen.gif)

(2)电影点播效果

![image](https://github.com/lijundacom/AndroidRTSPLib/blob/master/movie.gif)

六、参考

    1、参考fyhertz的libstreaming制作
    2、libstreaming连接：
    
              https://github.com/fyhertz/libstreaming

******************************************************************************************************************************              

1. Introduction

    AndroidRTSPLib is an API that allows you, with only a few lines of code, 
    to stream the screen frame, camera frame and movie with RTSP stream.
    Need Android 6.0 and above.

2. Function

    1) transform the android screen frame to streaming media.
    
    2) transform the android camera frame to streaming media.
    
    3) transform the mp4,3gp and avi movie to streaming media.


3. Usage:

    1) the AndroidRTSPLib is a rtsp server. Some media player supporting rtsp can be used as client, for example VLC player.
    
    2) the client player need to input URL with the following form
    
         rtsp://ip:port/option
         
    3) ip: the ip address of server(AndroidRTSPLib)
    
       port: the port of server(AndroidRTSPLib)
       
       option: inclued  screen, camera and movie
       
    4) port:   the default port is 1234, you can change it int source code,
    
               in file "MainActivity.java"
               
                  "private int mOriginPort = 1234;" to whatever you want.
                  
       screen: the client(media player) can display server's screen image live.
       
       camera: the client(media player) can display server's camera image live.
       
       movie:  the client(media player) can display movie on server's SD card.
       
               but first, you need rename your movie as "movie.mp4" and put the movie int the your SD card path with
               
                  "(SDcard root path)/AndroidRTSPLib/movie.mp4"
                  
               if you want to play a 3gp or avi movie, you need to change the source code like:
               
                   int the file “MainActivity.java”:
                   change
                   “VIDEO_PATH = SDCARD_PATH+"/AndroidRTSPLib/movie.mp4";”
                   to 
                   “VIDEO_PATH = SDCARD_PATH+"/AndroidRTSPLib/movie.3gp";” 
                   or 
                   “VIDEO_PATH = SDCARD_PATH+"/AndroidRTSPLib/movie.avi";”
                   
    5) you can put the URL int your media player, remember to change ip and port.
    
              rtsp://192.168.1.20:1234/screen   
              rtsp://192.168.1.20:1234/camera   
              rtsp://192.168.1.20:1234/movie    
              rtsp://192.168.1.20:1234          
              
4. Reference：

   The lib is based on Mr fyhertz's libstreaming
   
              https://github.com/fyhertz/libstreaming    
