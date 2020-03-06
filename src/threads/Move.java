package threads;

import io.Out;
import lejos.robotics.geometry.Line;
import lejos.robotics.localization.PoseProvider;
import lejos.robotics.mapping.LineMap;
import lejos.robotics.navigation.MovePilot;
import lejos.robotics.navigation.Pose;
import robot.Main;
import sensors.Infrared;

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
    
    private class ObstacleSensor extends Thread {
        private volatile boolean done = false;
        
        private Move m;
        
        public ObstacleSensor(Move m) {
            this.m = m;
        }

        @Override
        public void run() {
            while (!done) {
                Pose currentPose = pp.getPose();
                double deg = currentPose.getHeading();
                double rad = Math.toRadians(deg);
                
                double x1 = currentPose.getX();
                double y1 = currentPose.getY();

                double hyp = 17;
                double x2 = Math.cos(rad) * hyp + x1;
                double y2 = Math.sin(rad) * hyp + y1;
                
                //Line robotHeadingLine =
                  //      new Line((float)x1, (float)y1, (float)x2, (float)y2);
                
                for (Line l : map.getLines()) {
                    if (l.intersectsLine(x1, y1, x2, y2)) {
                        Main.setMoveLock(true);
                        m.exit();
                    }
                }
                
                if (inf.distanceLimitReached(100)) {
                    Main.setMoveLock(true);
                    m.exit();
                }
            }
        }

        public void exit() {
            this.done = true;
        }
    }

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