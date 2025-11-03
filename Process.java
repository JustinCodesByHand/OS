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
                runSemaphore.acquire();
                
                if (this instanceof UserlandProcess) {
                    UserlandProcess up = (UserlandProcess) this;
                    PCB pcb = up.getPCB();
                    if (pcb != null) {
                        PCB.setCurrent(pcb);
                    } else {
                        System.out.println("WARNING - PCB is null!");
                    }
                }
                
                main();
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } finally {
                if (this instanceof UserlandProcess) {
                   
                    PCB.setCurrent(null);
                }
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