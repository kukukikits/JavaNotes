#coding=utf-8
import cv2
import time
import subprocess as sp
import requests
import json
import platform
import threading
import signal
import sys
import traceback
import redis
import queue
import struct
import ffmpeg
import numpy as np
from threading import Event
# https://www.dazhuanlan.com/2019/12/16/5df679224a5d1/

# 线程共享全局变量
currentSourceFrame = None
currentResultFrame = None
rc = redis.StrictRedis(host='10.0.30.77', port='36379', db=3, password='shnXYaDmjxxXG-UP3ZC4x5iPDbDL6apJWwjVZAOde3Vd80T4nECzIw-2t7WrceEg')

class VideoCapture:

  def __init__(self, name):
    self.cap = cv2.VideoCapture(name)
    self.q = queue.Queue()
    t = threading.Thread(target=self._reader)
    t.daemon = True
    t.start()

  # read frames as soon as they are available, keeping only most recent one
  def _reader(self):
    while True:
      ret, frame2 = self.cap.read()
      if not ret:
        break
      if not self.q.empty():
        try:
          self.q.get_nowait()   # discard previous (unprocessed) frame
        except queue.Empty:
          pass
      self.q.put(frame2)

  def read(self):
    return self.q.get()

def packImage(img, timestamp=None):
    h, w = img.shape[:2]
    shape = struct.pack('>II', h, w)
    img_time = struct.pack('>d', timestamp or time.time())
    encoded = shape + img_time + img.tobytes()
    return encoded


def printException(Exception, e):
    print('str(Exception):\t', str(Exception))
    print('str(e):\t\t', str(e))
    print('repr(e):\t', repr(e))
    # Get information about the exception that is currently being handled  
    exc_type, exc_value, exc_traceback = sys.exc_info() 
    print('e.message:\t', exc_value)
    print("Note, object e and exc of Class %s is %s the same." % 
              (type(exc_value), ('not', '')[exc_value is e]))
    print('traceback.print_exc(): ', traceback.print_exc())
    print('traceback.format_exc():\n%s' % traceback.format_exc())
    print('########################################################')

class ImageReadingThread (threading.Thread):
    def __init__(self, ffmpegPath, sourceStream, rtspUrl, rtspHttpServer, frameEvent):
        threading.Thread.__init__(self)
        self.setDaemon(True)
        self.name = "ImageReadingThread"
        self.ffmpegPath = ffmpegPath
        self.sourceStream = sourceStream
        self.rtspUrl = rtspUrl
        self.rtspHttpServer = rtspHttpServer
        self.camera = None
        self.pushingThread = None
        self.frameEvent = frameEvent
    def run(self):
        while True:
            try:
                #self.readSource()
                self.read()
            except Exception as e:
                printException(Exception, e)
                print(self.name + ' catch exception. Sleep 10s then retry.')
                time.sleep(10)  
    def read(self):
        process = (
            ffmpeg
            .input(self.sourceStream, vsync=0, hwaccel='cuvid', vcodec='h264_cuvid', rtsp_transport='tcp',allowed_media_types='video',probesize=32,analyzeduration=0)
            .output('pipe:', format='rawvideo', pix_fmt='bgr24', vcodec='h264_nvenc')
            .run_async(pipe_stdout=True)
        )
        while True:
            in_bytes = process.stdout.read(480*640*3)
            if not in_bytes:
                break
            #rc.publish("living_stream", in_bytes)
            # in_frame = (
            #     np
            #     .frombuffer(in_bytes, np.uint8)
            #     .reshape([640, 480, 3])
            # )
            # rc.publish("living_stream", packImage(in_frame))

    def readSource(self):
        # 如果是流媒体，从服务器监测指定流媒体sourceStream是否存在
        # if self.sourceStream.startswith('rtsp') or self.sourceStream.startswith('rtmp'):
        #     while self.preCheckConnection(self.rtspHttpServer, self.sourceStream) != 0:
        #         print('Can\'t get sourceStream information. Wait for 10 seconds then retry ')
        #         time.sleep(10)
        if self.camera:
            self.camera.release()
            self.camera = None

        videoCapture = VideoCapture(sourceStream)
        camera = videoCapture.cap
        #camera.set(cv2.CAP_PROP_BUFFERSIZE, 3)

        #camera = cv2.VideoCapture(sourceStream) # 从sourceStream读取视频
        self.camera = camera
        if not camera.isOpened():
            print('rtsp stream is not available')
            return
        
        # 视频属性
        size = (int(camera.get(cv2.CAP_PROP_FRAME_WIDTH)), int(camera.get(cv2.CAP_PROP_FRAME_HEIGHT)))
        if size[0] <= 0 or size[1] <= 0:
            print('frame can\'t be 0x0')
            return
        sizeStr = str(size[0]) + 'x' + str(size[1])
        fps = camera.get(cv2.CAP_PROP_FPS)  # 30p/self
        fps = int(fps)
        if fps < 0 or fps > 60:
            print('Can\'t get fps of stream, using default 25 fps')
            fps = 25
        hz = int(1000.0 / fps)
        print('size:'+ sizeStr + ' fps:' + str(fps) + ' hz:' + str(hz))
        #self.startPushingThread(self.ffmpegPath, sizeStr, fps, self.rtspUrl, self.frameEvent, self.rtspHttpServer)
        global currentSourceFrame
        while True:
            frame = videoCapture.read()
            # ret, frame = camera.read() # 逐帧采集视频流
            #if ret:
            currentSourceFrame = frame
            rc.publish("living_stream", packImage(frame))
                #self.frameEvent.set() #--> 发送事件
            # else:
            #     currentSourceFrame = None
            #     return
    def startPushingThread(self, ffmpegPath, size, fps, rtspUrl, frameEvent, rtspHttpServer):
        if self.pushingThread:
            return
        self.pushingThread = ResultPushingThread(ffmpegPath, size, fps, rtspUrl, frameEvent, rtspHttpServer)
        self.pushingThread.setDaemon(True)
        self.pushingThread.start()
    def preCheckConnection(self, rtspHttpServer, sourceStream):
        queries = sourceStream.split('/')
        query = queries[len(queries) - 1]
        requestUrl = rtspHttpServer + "/api/v1/pushers?q=" + query
        print('Request url: ', requestUrl)
        res = requests.get(requestUrl)
        body = json.loads(res.text)
        print('Rtsp http server response: ', body)
        if res and res.status_code == 200 and body['total'] >= 1:
            for row in body['rows']:
                if row['source'].endswith(sourceStream):
                    return 0
            return -1
        else:
            return -1

class ImageProcessThread(threading.Thread):
    def __init__(self, frameEvent):
        threading.Thread.__init__(self)
        self.name = "ImageProcessThread"
        self.setDaemon(True)
        self.frameEvent = frameEvent
    def run(self):
        while True:
            try:
                self.process()
            except Exception as e:
                printException(Exception, e)
                print(self.name + ' catch exception. Sleep 10s then retry.')
                time.sleep(10)

    def process(self):
        while True:
            global currentSourceFrame
            global currentResultFrame
            # 将线程共享变量保存到本地变量frame上，使用frame作为程序输入
            frame = currentSourceFrame
            if not frame is None:
                # 处理图片
                # currentResultFrame = 处理后的图片
                # self.frameEvent.set() #--> 发送事件

                # 模拟处理过程
                time.sleep(1/15)
                currentResultFrame = frame
                #self.frameEvent.set() #--> 发送事件
            else:
                time.sleep(10)    
       
class ResultPushingThread (threading.Thread):
    def __init__(self, ffmpegPath, size, fps, rtspUrl, frameEvent, rtspHttpServer):
        threading.Thread.__init__(self)
        self.setDaemon(True)
        self.name = "ResultPushingThread"
        self.ffmpegPath = ffmpegPath
        self.size = size
        self.fps = fps
        self.rtspUrl = rtspUrl
        self.pipe = None
        self.frameEvent = frameEvent
        self.rtspHttpServer = rtspHttpServer
        self.streamInfo = {}
    def stop(self):
        queries = self.rtspUrl.split('/')
        query = queries[len(queries) - 1]
        requestUrl = self.rtspHttpServer + "/api/v1/pushers?q=" + query
        print('Query stream info: ', requestUrl)
        res = requests.get(requestUrl)
        body = json.loads(res.text)
        print('Rtsp http server response: ', body)
        if res and res.status_code == 200 and body['total'] >= 1:
            for row in body['rows']:
                if row['source'] == self.rtspUrl:
                    self.streamInfo = row
                    break
        if self.streamInfo and self.streamInfo['id']:
            requestUrl = self.rtspHttpServer + "/api/v1/stream/stop?id=" + self.streamInfo['id']
            print('Stop pushing stream request: ', requestUrl)
            res = requests.get(requestUrl)
            print('Rtsp http server response: ', res.status_code, ' body:', res.text)
    def run(self): 
        while True:
            try: 
                self.push()
            except Exception as e:
                printException(Exception, e)
                print(self.name + ' catch exception. Sleep 10s then retry.')
                time.sleep(10)    
            finally:
                if self.pipe:
                    self.pipe.terminate()
                    self.pipe = None    
    def push(self):
        # 直播管道输出 
        # ffmpeg推送rtsp 重点 ： 通过管道 共享数据的方式
        command = [self.ffmpegPath,
            '-y',
            '-f', 'rawvideo',
            '-vcodec','rawvideo',
            '-pix_fmt', 'bgr24',
            '-s', self.size,
            '-r', str(self.fps), # 默认fps是25
            '-i', '-',
            # https://github.com/arut/nginx-rtmp-module/issues/378#issuecomment-323592252
            '-c:v', 'libx264',
            '-b:v', '500k', # 码率
            '-crf', '30',   # 0 to 63, 数字越大表示质量越低，输出大小越小
            '-intra-refresh', '1',
            # https://stackoverflow.com/questions/43868982/low-latency-dash-nginx-rtmp/45370210#45370210
            '-g', '50',     # 设置关键帧间隔， The default GOP size for ffmpeg is 250 which means there will be a key frame every 250 frames.
            '-pix_fmt', 'yuv420p',
            '-preset', 'ultrafast',
            '-tune', 'zerolatency',
            #'-rtsp_transport', 'tcp',
            #'-f', 'rtsp', self.rtspUrl,
            # "-maxrate", "750k",
            # "-bufsize", "3000k",
            # "-movflags", "+faststart",
            # "-x264opts", "opencl",
            "-f", "mpegts", "udp://10.1.100.9:2000"
        ]

        if self.pipe:
            self.pipe.terminate()
        #管道特性配置
        self.pipe = sp.Popen(command, stdin=sp.PIPE)   

        global currentResultFrame
        while True:
            self.frameEvent.wait() # 等待图像处理结果
            frame = currentResultFrame
            if not frame is None:
                # 用ffmpeg工具将结果帧推流到rtsp流媒体服务器
                self.pipe.stdin.write(frame.tobytes())  # 存入管道用于直播
            else:
                time.sleep(10)
            self.frameEvent.clear() #将event的标志设置为False，调用wait方法的所有线程将被阻塞； 


# 替换ffmpeg的路径
system = platform.system()
if system == "Windows":
    ffmpegPath = "D:/Programs/ffmpeg-4.3.1-2021-01-26-full_build/bin/ffmpeg.exe"
elif system == "Linux":
    ffmpegPath = "ffmpeg"

sourceStream = 'rtsp://10.1.100.9:554/stream' # 源视频流地址
rtspHttpServer = 'http://10.1.100.9:10008'
#sourceStream = 'rtsp://10.0.40.92:554/uvc.source.sdp' # 源视频流地址
rtspUrl = 'rtsp://10.0.40.92:554/test.result' # 处理完后视频后，将车道线识别结果(png图片)推流到这个地址
#rtspHttpServer = 'http://10.0.40.92:10008'
#sourceStream = 'rtsp://localhost:554/test.source' # 源视频流地址
#rtspUrl = 'rtsp://localhost:554/test' # 处理完后视频后，将车道线识别结果(png图片)推流到这个地址
#rtspHttpServer = 'http://localhost:10008'
pushingThread = None
def exitHandler(signum, frame):
    if not pushingThread is None:
        pushingThread.stop()
    exit()

def run():
    
    frameEvent = Event()
    readingThread = ImageReadingThread(ffmpegPath, sourceStream, rtspUrl, rtspHttpServer, frameEvent)
    processThread = ImageProcessThread(frameEvent)
    readingThread.setDaemon(True)
    processThread.setDaemon(True)
    readingThread.start()
    processThread.start()
    signal.signal(signal.SIGINT, exitHandler)
    signal.signal(signal.SIGTERM, exitHandler)

    global pushingThread
    while True:
        if pushingThread is None:
            pushingThread = readingThread.pushingThread
        pass
    
run()


