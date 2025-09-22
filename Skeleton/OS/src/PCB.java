
 // Process Control Block
 // data structure used by the kernel to store data about a process

public class PCB {
    private static int nextPid = 1; // counter for process IDs
    private final int pid; // process's unique ID
    private final UserlandProcess userlandProcess; // reference to the actual process object
    private OS.PriorityType priority;
    private String name;
    private long wakeupTime = 0; // For sleeping
    private int timeoutCount = 0; // For demotion

    /**
     * Constructor for a new PCB.
     * @param up The userland process this PCB will manage.
     * @param priority The priority of the process.
     */
    PCB(UserlandProcess up, OS.PriorityType priority) {
        this.pid = nextPid++;
        this.userlandProcess = up;
        this.priority = priority;
        this.name = up.getClass().getSimpleName() + "-" + pid;
        System.out.println("PCB created: " + getName());
    }

    public String getName() {
        return name;
    }

    public int getPid() {
        return pid;
    }

    public UserlandProcess getProcess() {
        return userlandProcess;
    }

    OS.PriorityType getPriority() {
        return priority;
    }

    public void setPriority(OS.PriorityType newPriority) {
        this.priority = newPriority;
    }


    //calls a requestStop call to userland process.
    public void requestStop() {
        userlandProcess.requestStop();
    }

    //calls a stop call to the underlying userland process
    public void stop() {
        userlandProcess.stop();
    }

    //checks if the underlying userland process's thread is finished
    public boolean isDone() {
        return userlandProcess.isDone();
    }

    //passes a start call to the underlying userland process
    void start() {
        userlandProcess.start();
    }



    // Add getters and setters for the new fields
    //public OS.PriorityType getPriority() { return priority; }
    //public void setPriority(OS.PriorityType p) { this.priority = p; }
    public long getWakeupTime() { return wakeupTime; }
    public void setWakeupTime(long t) { this.wakeupTime = t; }
    public int getTimeoutCount() { return timeoutCount; }
    public void setTimeoutCount(int c) { this.timeoutCount = c; }
}