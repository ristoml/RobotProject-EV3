package threads;

/**
 * Abstract class representing some behavior of the robot,
 * which would run in a separate thread.
 */
public abstract class RobotAction extends Thread {

    protected volatile boolean done = false;

    public abstract void action() throws Exception;

    @Override
    public void run() {
        try {
            action();
        } catch (Exception e) {
            System.exit(1);
        }
    }

    public boolean isRunning() {
        return !this.done;
    }

    public void exit() {
        this.done = true;
    }
}