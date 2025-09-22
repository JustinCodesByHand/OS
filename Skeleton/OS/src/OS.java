import java.util.ArrayList;
import java.util.List;


 //the OS class allows userland processes to make system calls
 // it acts like a bridge between unprivileged processes and the privileged kernel

public class OS {
    private static Kernel ki;

    // shared memory for parameters and return values
    public static List<Object> parameters = new ArrayList<>();
    public static Object retVal;

    // define the types of system calls available
    public enum CallType {SwitchProcess,SendMessage, Open, Close, Read, Seek, Write, GetMapping, CreateProcess, Sleep, GetPID, AllocateMemory, FreeMemory, GetPIDByName, WaitForMessage, Exit}
    public static CallType currentCall;


     //wakes up the kernel to handle a pending system call

    private static void startTheKernel() {
        if (ki == null) {
            ki = new Kernel();
        }
        // releases the kernel's semaphore, lets it run
        ki.start();
    }

    /**
     * The startup sequence for the OS.
     * @param init The first userland process to be created.
     */
    public static void Startup(UserlandProcess init) {
        // create the Init process, it will create other processes.
        CreateProcess(init, PriorityType.interactive);
        // create the IDle process, for when nothing is happening
        CreateProcess(new IdleProcess(), PriorityType.background);
    }

    // defines the priority levels for processes
    public enum PriorityType {realtime, interactive, background}

    /**
     * the system call for creating a new process
     * @param priority the priority of the new process
     * @return the process ID of the new  process
     */
    public static int CreateProcess(UserlandProcess up, PriorityType priority) {
        // reset the return value to prepare
        retVal = null;
        // set up the params for kernel
        parameters.clear();
        parameters.add(up);
        parameters.add(priority);
        currentCall = CallType.CreateProcess;

        // wake up the kernel
        startTheKernel();

        // wait here until the kernel has finished and set the return value
        // stops race condition at startup
        while (retVal == null) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return (int) retVal;
    }

    public static void switchProcess() {
        currentCall = CallType.SwitchProcess;
        // wake up the kernel to switch, do not wait
        startTheKernel();
    }




    public static int GetPID() {
        currentCall = CallType.GetPID;
        startTheKernel();
        // This must block and wait for the kernel to set the return value.
        while (retVal == null) {
            try { Thread.sleep(10); } catch (InterruptedException e) {}
        }
        return (int) retVal;
    }

    public static void Exit() {
        currentCall = CallType.Exit;
        startTheKernel();
    }

    public static void Sleep(int milliseconds) {
        parameters.clear();
        parameters.add(milliseconds);
        currentCall = CallType.Sleep;
        startTheKernel();
    }

    // Devices
    public static int Open(String s) {
        return 0;
    }

    public static void Close(int id) {
    }

    public static byte[] Read(int id, int size) {
        return null;
    }

    public static void Seek(int id, int to) {
    }

    public static int Write(int id, byte[] data) {
        return 0;
    }

    // Messages
    public static void SendMessage(KernelMessage km) {
    }

    public static KernelMessage WaitForMessage() {
        return null;
    }

    public static int GetPidByName(String name) {
        return 0; // Change this
    }

    // Memory
    public static void GetMapping(int virtualPage) {
    }

    public static int AllocateMemory(int size ) {
        return 0; // Change this
    }

    public static boolean FreeMemory(int pointer, int size) {
        return false; // Change this
    }
}

