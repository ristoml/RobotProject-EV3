package io;

import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfInt;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;

/**
 * A thread solely intended for capturing web camera-images and sending them to the PC-client.
 * The code for capturing video was inspired from this article:
 * https://lejosnews.wordpress.com/2015/09/26/opencv-web-streaming/
 */
public class VideoOut extends Thread {

    private volatile boolean done;
    private Out out;
    private int frameWidth;
    private int frameHeight;

    public VideoOut(Out out, int frameWidth, int frameHeight) {
        this.out = out;
        this.frameWidth = frameWidth;
        this.frameHeight = frameHeight;
    }

    @Override
    public void run() {
        Mat mat = new Mat();
        VideoCapture vid = new VideoCapture(0);
        vid.set(Highgui.CV_CAP_PROP_FRAME_WIDTH, frameWidth);
        vid.set(5, 15);
        vid.set(Highgui.CV_CAP_PROP_FRAME_HEIGHT, frameHeight);
        vid.open(0);
        System.out.println("Camera open");

        while (!done) {
            vid.read(mat);
            
            if (!mat.empty()) {
                MatOfByte buf = new MatOfByte();
                //Highgui.imencode(".jpg", mat, buf);
                MatOfInt params = new MatOfInt(Highgui.CV_IMWRITE_JPEG_QUALITY, 95);
                Highgui.imencode(".jpg", mat, buf, params);
                byte[] imageBytes = buf.toArray();

                out.write(Out.SEND_PICTURE, (Object)imageBytes);
            }
        }
    }
    
    public void exit() {
        done = true;
    }
}
