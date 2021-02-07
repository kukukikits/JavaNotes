import cv2

# 播放rtsp视频流
cap = cv2.VideoCapture("rtsp://localhost:554/uvc.result")
while(cap.isOpened()):
    ret, frame = cap.read()
    cv2.imshow('frame', frame)
    if cv2.waitKey(20) & 0xFF == ord('q'):
        break
cap.release()
cv2.destroyAllWindows()
