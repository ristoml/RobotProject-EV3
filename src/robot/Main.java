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
import lejos.robotics.geometry.Line;
import lejos.robotics.localization.PoseProvider;
import lejos.robotics.mapping.LineMap;
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
    
    private static LineMap map;

    public static void main(String[] args) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
        map = testMap();

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
        pilot.setAngularSpeed(100);
        //pilot.setLinearAcceleration(100);
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

    private static volatile boolean moveLock = false;
    
    public static synchronized void setMoveLock(boolean lock) {
        moveLock = lock;
    }
    
    private static void startRobot() throws IOException {
        RobotAction currentAction = null;

        int code;
        do {
            code = in.readInt();
            
            switch (code) {
                case MOVE_FORWARD:
                    if (moveLock || inf.distanceLimitReached(100)) {
                        System.out.println("limit reached");
                        continue;
                    }
                case MOVE_BACKWARD:
                    setMoveLock(false);
                case TURN_LEFT:
                    setMoveLock(false);
                case TURN_RIGHT:
                    if (code == TURN_RIGHT) {
                        setMoveLock(false);
                    }
                    if (currentAction != null) {
                        currentAction.exit();
                    }
                    currentAction = new Move(code, pilot, out, poseProvider, inf, map);
                    currentAction.start();
                    break;
                    
                case NAVIGATE:
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
    
    private static LineMap testMap() {
    lejos.robotics.geometry.Rectangle alue =
        new lejos.robotics.geometry.Rectangle(0, 0, 150, 150);
        Line[] esteet = new Line[12];

        // reunat
        esteet[0] = new Line(0, 0, 150, 0);
        esteet[1] = new Line(150, 0, 150, 150);
        esteet[2] = new Line(0, 150, 150, 150);
        esteet[3] = new Line(0, 0, 0, 150);

        // väliseinä1
        esteet[4] = new Line(50, 40, 60, 40);
        esteet[5] = new Line(60, 40, 60, 110);
        esteet[6] = new Line(50, 110, 60, 110);
        esteet[7] = new Line(50, 40, 50, 110);

        // väliseinä2
        esteet[8] = new Line(100, 40, 110, 40);
        esteet[9] = new Line(110, 40, 110, 110);
        esteet[10] = new Line(100, 110, 110, 110);
        esteet[11] = new Line(100, 40, 100, 110);

        return new LineMap(esteet, alue);
    }

}
