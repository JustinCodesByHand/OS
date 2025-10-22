// Process.java
import java.util.concurrent.Semaphore;

public abstract class Process implements Runnable{
    private final Thread thread;
    private final Semaphore runSemaphore;
    private boolean quantumExpired;
    private volatile boolean running;

    public Process() {
        runSemaphore = new Semaphore(0);
        quantumExpired = false;
        running = false;
        thread = new Thread(this);
        thread.start();
    }

    public void requestStop() {
        quantumExpired = true;
    }

    public abstract void main();

    public boolean isStopped() {
        return !running;
    }

    public boolean isDone() {
        return !thread.isAlive();
    }

    public void start() {
        running = true;
        runSemaphore.release();
    }

    public void stop() {
        try {
            this.runSemaphore.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                OS.currentProcess.set(this);
                runSemaphore.acquire();
                main();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public void cooperate() {
        if (quantumExpired) {
            quantumExpired = false;
            OS.switchProcessQuantum();
        }
    }
}