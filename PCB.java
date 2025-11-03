import java.util.LinkedList;

public class PCB {
    private static int nextPid = 1;
    private int pid;
    private UserlandProcess userlandProcess;
    private OS.PriorityType priority;
    private String name;

    private int timeoutCount = 0;
    private boolean demoted = false;

    private int[] deviceIds = new int[10];

    // For storing syscall return values per-process
    public Object syscallReturnValue = null;

    public LinkedList<KernelMessage> messageQueue = new LinkedList<>();

    // Page table: index is virtual page number, value is physical page number
    // -1 means no mapping exists for that virtual page
    private int[] pageTable = new int[100];
    
    // Thread-local storage to track which PCB owns which thread
    private static ThreadLocal<PCB> currentPCB = new ThreadLocal<>();

    public PCB(UserlandProcess up, OS.PriorityType priority) {
        this.pid = nextPid++;
        this.userlandProcess = up;
        this.priority = priority;
        this.name = up.getClass().getSimpleName() + "-" + pid;
        
        // Set this PCB in the UserlandProcess so it can use thread-local storage
        up.setPCB(this);
        
        // Initialize device IDs
        for (int i = 0; i < 10; i++) {
            deviceIds[i] = -1;
        }
        
        // Initialize page table - all pages unmapped
        for (int i = 0; i < 100; i++) {
            pageTable[i] = -1;
        }
    }
    
    public static PCB getCurrent() {
        return currentPCB.get();
    }
    
    public static void setCurrent(PCB pcb) {
        currentPCB.set(pcb);
    }

    public int getPid() {
        return pid;
    }

    public String getName() {
        return name;
    }

    public int[] getDeviceIds() {
        return deviceIds;
    }
    
    public int[] getPageTable() {
        return pageTable;
    }

    public OS.PriorityType getPriority() {
        return priority;
    }

    public int getTimeoutCount() {
        return timeoutCount;
    }

    public void incTimeoutCount() {
        timeoutCount++;
    }

    public void resetTimeoutCount() {
        timeoutCount = 0;
    }

    public void demoteIfNeeded() {
        if (timeoutCount > 5) {
            if (priority == OS.PriorityType.realtime) {
                priority = OS.PriorityType.interactive;
                demoted = true;
            } else if (priority == OS.PriorityType.interactive) {
                priority = OS.PriorityType.background;
                demoted = true;
            }
            timeoutCount = 0;
        }
    }

    public boolean wasDemoted() {
        return demoted;
    }

    public void start() {
        userlandProcess.start();
    }

    public void stop() {
        userlandProcess.stop();
    }
    
    public UserlandProcess getUserlandProcess() {
        return userlandProcess;
    }

    public boolean isDone() {
        return userlandProcess.isDone();
    }

    public void requestStop() {
        userlandProcess.requestStop();
    }
}