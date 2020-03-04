package io;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import lejos.robotics.navigation.Pose;

public class Out extends Thread {
    
    public static final int SEND_POSITION = 1;
    public static final int SEND_PICTURE = 2;
    
    private volatile boolean done;
    
    private DataOutputStream out;
    
    public Out(Socket socket) throws IOException {
        this.out = new DataOutputStream(socket.getOutputStream());
        this.done = false;
    }
    
    @Override
    public void run() {
        while (!done);
    }

    public void write(int type, Object data) {
        switch (type) {
            case SEND_PICTURE:
                byte[] imageBytes = (byte[])data;
                int len = imageBytes.length;

                try {
                    synchronized (this) {
                        out.writeInt(SEND_PICTURE);
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
    
    public void exit() {
        try {
            out.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        this.done = true;
    }
}
