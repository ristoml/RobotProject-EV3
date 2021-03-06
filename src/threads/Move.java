package threads;

import io.Out;
import lejos.robotics.geometry.Line;
import lejos.robotics.localization.PoseProvider;
import lejos.robotics.mapping.LineMap;
import lejos.robotics.navigation.MovePilot;
import lejos.robotics.navigation.Pose;
import robot.Main;
import sensors.Infrared;

/**
 * Moves the robot based on user input. Is a RobotAction, and would run as a separate thread.
 */
public class Move extends RobotAction {

    private Out out;
    private int code;
    private MovePilot pilot;
    private PoseProvider pp;
    private Infrared inf;
    private LineMap map;

    public Move(int code, MovePilot pilot, Out out, PoseProvider pp, Infrared inf, LineMap map) {
        this.code = code;
        this.pilot = pilot;
        this.out = out;
        this.pp = pp;
        this.inf = inf;
        this.map = map;
    }

    @Override
    public void action() throws Exception {
        ObstacleSensor os = null;
        
        switch (code) {
            case Main.MOVE_FORWARD:
                os = new ObstacleSensor(this);
                if (os.checkForObstacles()) { // initial check before moving
                    this.exit();
                    return; // already facing an obstacle, return immediately
                }
                os.start();
                pilot.forward();
                break;
            case Main.MOVE_BACKWARD:
                pilot.backward();
                break;
            case Main.TURN_LEFT:
                pilot.arc(0, 360, true);
                break;
            case Main.TURN_RIGHT:
                pilot.arc(0, -360, true);
                break;
        }

        PosSender ps = new PosSender();
        ps.start();

        while (this.isRunning())
            ;
        pilot.stop();
        ps.exit();
        if (os != null) os.exit();
    }
    
    /**
     * A private class this class uses for checking for obstacles in a separate thread.<br>
     * Obstacles could be physical objects detected by infrared sensor, or lines in robot's area defined by a LineMap.
     */
    private class ObstacleSensor extends Thread {
        private volatile boolean done = false;
        
        private Move m;
        
        public ObstacleSensor(Move m) {
            this.m = m;
        }
        
        /**
         * Checks both for physical objects and lines defined by robot's LineMap.
         * @return true if obstacle detected, false otherwise.
         */
        public boolean checkForObstacles() {
            return checkWithInfrared() || checkForIntersect();
        }
        
        /**
         * This method calculates coordinates for a short line directly in front of robot's current heading.<br>
         * It then loops through all the lines of the robot's linemap, checking for intersection with the calculated line.<br>
         * An intersection means the robot is about to cross a border, and should stop.
         * @return true if intersection detected, false otherwise.
         */
        private boolean checkForIntersect() {
            Pose currentPose = pp.getPose();
            double deg = currentPose.getHeading();
            double rad = Math.toRadians(deg);
            
            double x1 = currentPose.getX();
            double y1 = currentPose.getY();

            double hyp = 17;
            double x2 = Math.cos(rad) * hyp + x1;
            double y2 = Math.sin(rad) * hyp + y1;

            for (Line l : map.getLines()) {
                if (l.intersectsLine(x1, y1, x2, y2)) {
                    return true;
                }
            }
            return false;
        }
        
        /**
         * Checks if the robot is near a physical object by using its infrared sensor.
         * @return
         */
        private boolean checkWithInfrared() {
            return inf.distanceLimitReached(100);
        }

        @Override
        public void run() {
            while (!done) {
                
                if (checkForObstacles()) {
                    m.exit();
                }
            }
        }

        public void exit() {
            this.done = true;
        }
    }

    /**
     * A private class this class uses for sending robot's current position to the PC-client.
     */
    private class PosSender extends Thread {
        private volatile boolean done = false;

        @Override
        public void run() {
            while (!done) {
                Pose pose = pp.getPose();
                out.write(Out.SEND_POSITION, (Object) pose);

                try {
                    sleep(50);
                } catch (InterruptedException e) {
                }
            }
        }

        public void exit() {
            this.done = true;
        }
    }
}