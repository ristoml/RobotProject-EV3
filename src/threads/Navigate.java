package threads;

import io.Out;
import lejos.robotics.chassis.Chassis;
import lejos.robotics.geometry.Line;
import lejos.robotics.localization.PoseProvider;
import lejos.robotics.mapping.LineMap;
import lejos.robotics.navigation.MovePilot;
import lejos.robotics.navigation.Navigator;
import lejos.robotics.navigation.Pose;
import lejos.robotics.navigation.Waypoint;
import lejos.robotics.pathfinding.Path;
import lejos.robotics.pathfinding.ShortestPathFinder;

public class Navigate extends RobotAction {
    
    private Path path;
    private MovePilot pilot;
    private Out out;
    private PoseProvider pp;
    
    public Navigate(Path path, MovePilot pilot, Out out, Chassis chassis) {
        this.path = path;
        this.pilot = pilot;
        this.out = out;
        this.pp = chassis.getPoseProvider();
    }

    @Override
    public void action() throws Exception {
        LineMap m = testMap();
        ShortestPathFinder pathFinder = new ShortestPathFinder(m);
        pathFinder.lengthenLines(20);
 
        Navigator navi = new Navigator(pilot, pp);
        
        PosSender psender = new PosSender();
        psender.start();
        
        for (Waypoint wp : path) {
            Pose currentPose = pp.getPose();
            Path path = pathFinder.findRoute(currentPose, wp);
            navi.followPath(path);
            navi.waitForStop();
        }
        psender.exit();
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
    
    private LineMap testMap() {
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
