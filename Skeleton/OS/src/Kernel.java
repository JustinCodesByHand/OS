//The Kernel is the core of the OS. It handles system calls and manages the scheduler.

public class Kernel extends Process {
    private final Scheduler scheduler;
    private PCB currentlyRunning;


    // constructor for the Kernel

    public Kernel() {
        this.scheduler = new Scheduler();
    }

    /**
     * The main loop of the Kernel:
     * 1. Waits to be awakened by a system call
     * 2. Handles the single pending call
     * 3. Schedules the next user process
     * 4. Starts the user process
     * 5. Puts itself back to sleep
     */
    @Override
    public void main() {
        while (true) {
            // handles a single system call like CreateProcess or SwitchProcess
            handleSystemCall();

            // tell the scheduler to pick the next process from queue
            currentlyRunning = scheduler.schedule();

            // if a process was chosen, start it; gives it the "talking stick"
            if (currentlyRunning != null) {
                currentlyRunning.start();
            }

            // puts the kernel to sleep, waiting for the semaphore
            this.stop();
        }
    }


     // check for sys call from the OS and sends it to the correct handler

    private void handleSystemCall() {
        if (OS.currentCall != null) {
            switch (OS.currentCall) {
                case CreateProcess:
                    handleCreateProcess();
                    break;
                case SwitchProcess:
                    // the kernel just needs to start again
                    // continues inn main loop
                    break;
                case GetPID:
                    // If a process is running, get its PID and set it as the return value.
                    if (currentlyRunning != null) {
                        OS.retVal = currentlyRunning.getPid();
                    }
                    break;
                case Exit:
                    // Tell the scheduler to terminate the current process.
                    scheduler.exit();
                    break;
                case Sleep:
                    // Unpack the milliseconds and tell the scheduler to put the process to sleep.
                    int milliseconds = (int) OS.parameters.get(0);
                    scheduler.sleep(milliseconds);
                    break;
            }
            // reset the call value
            OS.currentCall = null;
        }
    }

    private void handleCreateProcess() {
        // Unpack the parameters from the OS class.
        UserlandProcess up = (UserlandProcess) OS.parameters.get(0);
        OS.PriorityType priority = (OS.PriorityType) OS.parameters.get(1);

        // Delegate the actual creation to the scheduler and set the return value.
        // This unblocks the waiting OS.CreateProcess call.
        OS.retVal = scheduler.createProcess(up, priority);
    }






    private void SwitchProcess() {}

    // For assignment 1, you can ignore the priority. We will use that in assignment 2
    private int CreateProcess(UserlandProcess up, OS.PriorityType priority) {
        return 0; // change this
    }

    private void Sleep(int mills) {
    }

    private void Exit() {
    }

    private int GetPid() {
        return 0; // change this
    }

    private int Open(String s) {
        return 0; // change this
    }

    private void Close(int id) {
    }

    private byte[] Read(int id, int size) {
        return null; // change this
    }

    private void Seek(int id, int to) {
    }

    private int Write(int id, byte[] data) {
        return 0; // change this
    }

    private void SendMessage(/*KernelMessage km*/) {
    }

    private KernelMessage WaitForMessage() {
        return null;
    }

    private int GetPidByName(String name) {
        return 0; // change this
    }

    private void GetMapping(int virtualPage) {
    }

    private int AllocateMemory(int size) {
        return 0; // change this
    }

    private boolean FreeMemory(int pointer, int size) {
        return true;
    }

    private void FreeAllMemory(PCB currentlyRunning) {
    }

}