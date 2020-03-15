package io;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import lejos.robotics.navigation.Pose;

/**
 * A thread intended to handle all writes to output stream synchronously.
 */
public class Out extends Thread {
    
    /**
     * Constant for write method. Used when sending a {@link Pose}-object.
     */
    public static final int SEND_POSITION = 1;
    /**
     * Constant for write method. Used when sending a video frame as byte[].
     */
    public static final int SEND_VIDEO_FRAME = 2;
    
    private volatile boolean done;
    
    private DataOutputStream out;
    
    /**
     * Constructor that takes a socket, and gets its output stream.
     * @param socket
     * @throws IOException
     */
    public Out(Socket socket) throws IOException {
        this.out = new DataOutputStream(socket.getOutputStream());
        this.done = false;
    }
    
    @Override
    public void run() {
        while (!done);
    }

    /**
     * Writes an object of specified type (see constants of this class) to the output stream.
     * @param type see constants of this class
     * @param data data as Object.
     */
    public void write(int type, Object data) {
        switch (type) {
            case SEND_VIDEO_FRAME:
                byte[] imageBytes = (byte[])data;
                int len = imageBytes.length;

                try {
                    synchronized (this) {
                        out.writeInt(SEND_VIDEO_FRAME);
                        out.flush();
                        out.writeInt(len);
                        out.flush();
                        out.write(imageBytes, 0, len);
                        out.flush();
                    }
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                break;
                
            case SEND_POSITION:
                Pose pose = (Pose)data;
                try {
                    synchronized (this) {
                        out.writeInt(SEND_POSITION);
                        pose.dumpObject(out);
                    }
                } catch (IOException e) {
                    System.out.println(e.getMessage());
                }
                break;
            default:
        }
    }
    
    /**
     * End the life of a running Out-instance.
     */
    public void exit() {
        try {
            out.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        this.done = true;
    }
}
