package threads;

import io.Out;
import lejos.robotics.localization.PoseProvider;
import lejos.robotics.navigation.MovePilot;
import lejos.robotics.navigation.Pose;
import robot.Main;

public class Move extends RobotAction {

    private Out out;
    private int code;
    private MovePilot pilot;
    private PoseProvider pp;

    public Move(int code, MovePilot pilot, Out out, PoseProvider pp) {
        this.code = code;
        this.pilot = pilot;
        this.out = out;
        this.pp = pp;
    }

    @Override
    public void action() throws Exception {
        switch (code) {
            case Main.MOVE_FORWARD:
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