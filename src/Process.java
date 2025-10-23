import java.util.concurrent.Semaphore;

public abstract class Process implements Runnable{
    private final Thread thread;
    private final Semaphore runSemaphore; // controls when the process is allowed to run
    private boolean quantumExpired;
    private volatile boolean running;

    //constructor for new processes
    public Process() {
        // each process gets a semaphore that starts with 0 permits, so it won't run initially
        runSemaphore = new Semaphore(0);
        quantumExpired = false;
        running = false;
        // each process runs in its own thread
        thread = new Thread(this);
        thread.start();
    }

    //flags this process's time quantum has expired
    public void requestStop() {
        quantumExpired = true;
    }

    public abstract void main();

    public boolean isStopped() {
        return !running;
    }

    //checks if the process's underlying thread has terminated
    public boolean isDone() {
        return !thread.isAlive();
    }

    //allows the process to run by releasing its semaphore
    public void start() {
        running = true;
        runSemaphore.release();
    }

    //pauses the process by acquiring its semaphore, blocking the thread
    public void stop() {
        try {
            this.runSemaphore.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    //the start point for the process's thread
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                // the thread is blocked here until start() is called
                runSemaphore.acquire();
                // once unblocked, execute  main
                main();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }


     //called by a user process to yield control if its time is up

    public void cooperate() {
        if (quantumExpired) {
            quantumExpired = false;
            // make a system call to the OS to switch processes
            OS.switchProcessQuantum();
        }
    }
}