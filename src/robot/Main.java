package robot;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.opencv.core.Core;

import io.Out;
import io.VideoOut;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.port.MotorPort;
import lejos.robotics.RegulatedMotor;
import lejos.robotics.chassis.Chassis;
import lejos.robotics.chassis.Wheel;
import lejos.robotics.chassis.WheeledChassis;
import lejos.robotics.localization.PoseProvider;
import lejos.robotics.navigation.MovePilot;
import lejos.robotics.navigation.Pose;
import lejos.robotics.navigation.Waypoint;
import lejos.robotics.pathfinding.Path;
import sensors.Infrared;
import threads.Move;
import threads.Navigate;
import threads.RobotAction;

public class Main {

    private static final int PORT = 1111;

    public static final int STOP = -1;
    public static final int END_ROBOT_PROGRAM = -2;
    public static final int MOVE_FORWARD = 2;
    public static final int MOVE_BACKWARD = 3;
    public static final int TURN_LEFT = 4;
    public static final int TURN_RIGHT = 5;
    
    public static final int NAVIGATE = 6;

    private static PoseProvider poseProvider;
    static Chassis chassis;
    private static MovePilot pilot;
    //private static double diameter = 4.10;
    private static double diameter = 4.15;

    private static double offset = 6.49;
    //private static double offset = 6.45;
    // private static double diameter = 3.13;
    // private static double offset = 8.37;

    private static ServerSocket server;
    private static Socket socket;

    private static DataInputStream in;
    private static Out out;
    private static VideoOut vout;

    private static Infrared inf;

    public static void main(String[] args) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        Thread.currentThread().setPriority(Thread.MIN_PRIORITY);

        initSocket();
        initRobot();
        openIOstreams();

        try {
            startRobot();
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        } finally {
            closeIOstreams();
        }
    }

    private static void initSocket() {
        try {
            server = new ServerSocket(PORT);
            System.out.println("Server socket waiting for connection.");
            socket = server.accept();
        } catch (IOException e) {
            System.out.println("Connection failure. " + e.getMessage());
            System.exit(1);
        }
        System.out.println("Connected successfully.");
    }

    private static void initRobot() {
        inf = new Infrared("S4");

        RegulatedMotor left = new EV3LargeRegulatedMotor(MotorPort.A);
        RegulatedMotor right = new EV3LargeRegulatedMotor(MotorPort.D);

        Wheel leftWheel = WheeledChassis.modelWheel(left, diameter).offset(offset);
        Wheel rightWheel = WheeledChassis.modelWheel(right, diameter).offset(-offset);

        chassis = new WheeledChassis(new Wheel[] { leftWheel, rightWheel }, WheeledChassis.TYPE_DIFFERENTIAL);
        poseProvider = chassis.getPoseProvider();

        // TODO: poista testi
        //poseProvider.setPose(new Pose(20, 20, 0));

        Waypoint wp = new Waypoint(20, 20);
        poseProvider.setPose(wp.getPose());

        pilot = new MovePilot(chassis);
    }

    private static void openIOstreams() {
        try {
            in = new DataInputStream(socket.getInputStream());
            out = new Out(socket);
            out.start();

            final int frameWidth = 160, frameHeight = 120;
            vout = new VideoOut(out, frameWidth, frameHeight);
            vout.setPriority(Thread.MAX_PRIORITY);
            vout.start();
        } catch (IOException e) {
            System.out.println("Failed to open I/O streams.");
            System.exit(1);
        }
    }

    private static void startRobot() throws IOException {
        RobotAction currentAction = null;

        int code;
        do {
            code = in.readInt();

            switch (code) {
                case MOVE_FORWARD:
                case MOVE_BACKWARD:
                case TURN_LEFT:
                case TURN_RIGHT:
                    if (currentAction != null) {
                        currentAction.exit();
                    }
                    currentAction = new Move(code, pilot, out, poseProvider);
                    currentAction.start();
                    break;
                    
                case NAVIGATE:
                    System.out.println("nav");
                    if (currentAction != null) {
                        currentAction.exit();
                    }
                    Path path = new Path();
                    path.loadObject(in);
                    
                    currentAction = new Navigate(path, pilot, out, chassis);
                    currentAction.start();
                    try {
                        currentAction.join();
                    } catch (InterruptedException e) {
                        System.out.println(e.getMessage());
                    }
                    currentAction.exit();
                    currentAction = null;
                    break;

                case STOP:
                    if (currentAction != null) {
                        currentAction.exit();
                    }
                    currentAction = null;
                    break;
            }
        } while (code != END_ROBOT_PROGRAM);
    }

    private static void closeIOstreams() {
        try {
            socket.close();
        } catch (IOException ex) {
        }
        try {
            server.close();
        } catch (IOException ex) {
        }

        vout.exit();
        out.exit();
    }

}
