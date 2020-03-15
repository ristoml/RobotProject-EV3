package threads;

/**
 * Abstract class representing some behavior of the robot,
 * which would run in a separate thread.
 */
public abstract class RobotAction extends Thread {

    protected volatile boolean done = false;

    /**
     * Action that will run when thread starts.
     * @throws Exception
     */
    public abstract void action() throws Exception;

    @Override
    public void run() {
        try {
            action();
        } catch (Exception e) {
            System.exit(1);
        }
    }

    /**
     * @return true if an instance is running, false otherwise.
     */
    public boolean isRunning() {
        return !this.done;
    }

    /**
     * Ends the life of a RobotAction-instance.
     */
    public void exit() {
        this.done = true;
    }
}