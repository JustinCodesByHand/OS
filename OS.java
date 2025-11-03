import java.util.ArrayList;
import java.util.List;

public class OS {
    private static Kernel ki;
    public static List<Object> parameters = new ArrayList<>();
    public static Object retVal;
    public static PCB currentSyscallPCB;

    public enum CallType { None, CreateProcess, SwitchProcess, SwitchProcessQuantum, GetPID, Exit, Sleep, Open, Close, Read, Write, Seek, GetPidByName, SendMessage, WaitForMessage, AllocateMemory, FreeMemory, GetMapping }
    public static CallType currentCall;

    private static void startTheKernel() {
        if (ki == null) {
            ki = new Kernel(new Scheduler());
        }
        ki.start();
    }

    public static void Startup(UserlandProcess init) {
        CreateProcess(init, PriorityType.interactive);
        CreateProcess(new IdleProcess(), PriorityType.background);
    }

    public enum PriorityType { realtime, interactive, background }

    private static Object waitForReturnValue(PCB pcb) {
        if (pcb == null) return null;
        synchronized (pcb) {
            long timeout = System.currentTimeMillis() + 5000;
            while (pcb.syscallReturnValue == null && System.currentTimeMillis() < timeout) {
                try {
                    pcb.wait(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
            Object result = pcb.syscallReturnValue;
            pcb.syscallReturnValue = null;
            return result;
        }
    }

    public static int CreateProcess(UserlandProcess up, PriorityType priority) {
        synchronized (OS.class) {
            retVal = null;
            parameters.clear();
            parameters.add(up);
            parameters.add(priority);
            currentCall = CallType.CreateProcess;
            startTheKernel();

            while (retVal == null) {
                try {
                    OS.class.wait(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            return (int) retVal;
        }
    }

    public static void switchProcess() {
        synchronized (OS.class) {
            currentCall = CallType.SwitchProcess;
            startTheKernel();
        }
    }

    public static void switchProcessQuantum() {
        synchronized (OS.class) {
            currentCall = CallType.SwitchProcessQuantum;
            startTheKernel();
        }
    }

    public static void Sleep(int ms) {
        synchronized (OS.class) {
            parameters.clear();
            parameters.add(ms);
            currentCall = CallType.Sleep;
            startTheKernel();
        }
    }

    public static int GetPID() {
        PCB callingPCB;
        synchronized (OS.class) {
            if (ki == null || ki.scheduler == null) return -1;
            callingPCB = ki.scheduler.getCurrentlyRunning();
            if (callingPCB == null) return -1;

            synchronized (callingPCB) {
                callingPCB.syscallReturnValue = null;
            }

            parameters.clear();
            currentSyscallPCB = callingPCB;
            currentCall = CallType.GetPID;
            startTheKernel();
        }

        Object result = waitForReturnValue(callingPCB);
        return (result instanceof Integer) ? (int) result : -1;
    }

    public static void Exit() {
        PCB callingPCB;
        synchronized (OS.class) {
            if (ki == null || ki.scheduler == null) return;
            callingPCB = ki.scheduler.getCurrentlyRunning();
            if (callingPCB == null) return;
            synchronized (callingPCB) {
                callingPCB.syscallReturnValue = null;
            }
            parameters.clear();
            currentSyscallPCB = callingPCB;
            currentCall = CallType.Exit;
            startTheKernel();
        }
        waitForReturnValue(callingPCB);
    }

    // ---------------- File/Device Syscalls ----------------

    public static int Open(String deviceSpec) {
        PCB callingPCB = PCB.getCurrent();  // Use thread-local PCB
        synchronized (OS.class) {
            if (ki == null || ki.scheduler == null) return -1;
            if (callingPCB == null) return -1;

            synchronized (callingPCB) {
                callingPCB.syscallReturnValue = null;
            }

            parameters.clear();
            parameters.add(deviceSpec);
            currentSyscallPCB = callingPCB;
            currentCall = CallType.Open;
            startTheKernel();
        }

        Object result = waitForReturnValue(callingPCB);
        return (result instanceof Integer) ? (int) result : -1;
    }

    public static int Write(int id, byte[] data) {
        PCB callingPCB = PCB.getCurrent();  // Use thread-local PCB
        synchronized (OS.class) {
            if (ki == null || ki.scheduler == null) return 0;
            if (callingPCB == null) return 0;

            synchronized (callingPCB) {
                callingPCB.syscallReturnValue = null;
            }

            parameters.clear();
            parameters.add(id);
            parameters.add(data);
            currentSyscallPCB = callingPCB;
            currentCall = CallType.Write;
            startTheKernel();
        }

        Object result = waitForReturnValue(callingPCB);
        return (result instanceof Integer) ? (int) result : 0;
    }

    public static void Close(int id) {
        PCB callingPCB = PCB.getCurrent();  // Use thread-local PCB
        synchronized (OS.class) {
            if (ki == null || ki.scheduler == null) return;
            if (callingPCB == null) return;

            synchronized (callingPCB) {
                callingPCB.syscallReturnValue = null;
            }

            parameters.clear();
            parameters.add(id);
            currentSyscallPCB = callingPCB;
            currentCall = CallType.Close;
            startTheKernel();
        }

        waitForReturnValue(callingPCB);
    }

    public static byte[] Read(int id, int size) {
        PCB callingPCB = PCB.getCurrent();  // Use thread-local PCB
        synchronized (OS.class) {
            if (ki == null || ki.scheduler == null) return null;
            if (callingPCB == null) return null;

            synchronized (callingPCB) {
                callingPCB.syscallReturnValue = null;
            }

            parameters.clear();
            parameters.add(id);
            parameters.add(size);
            currentSyscallPCB = callingPCB;
            currentCall = CallType.Read;
            startTheKernel();
        }

        Object result = waitForReturnValue(callingPCB);
        return (result instanceof byte[]) ? (byte[]) result : null;
    }

    public static void Seek(int id, int to) {
        PCB callingPCB = PCB.getCurrent();  // Use thread-local PCB
        synchronized (OS.class) {
            if (ki == null || ki.scheduler == null) return;
            if (callingPCB == null) return;

            synchronized (callingPCB) {
                callingPCB.syscallReturnValue = null;
            }

            parameters.clear();
            parameters.add(id);
            parameters.add(to);
            currentSyscallPCB = callingPCB;
            currentCall = CallType.Seek;
            startTheKernel();
        }

        waitForReturnValue(callingPCB);
    }

    public static int GetPidByName(String name) {
        PCB callingPCB;
        synchronized (OS.class) {
            if (ki == null || ki.scheduler == null) return -1;
            callingPCB = ki.scheduler.getCurrentlyRunning();
            if (callingPCB == null) return -1;
            synchronized (callingPCB) {
                callingPCB.syscallReturnValue = null;
            }
            parameters.clear();
            parameters.add(name);
            currentSyscallPCB = callingPCB;
            currentCall = CallType.GetPidByName;
            startTheKernel();
        }
        Object result = waitForReturnValue(callingPCB);
        return (result instanceof Integer) ? (int) result : -1;
    }

    public static void SendMessage(KernelMessage km) {
        PCB callingPCB;
        synchronized (OS.class) {
            if (ki == null || ki.scheduler == null) return;
            callingPCB = ki.scheduler.getCurrentlyRunning();
            if (callingPCB == null) return;
            synchronized (callingPCB) {
                callingPCB.syscallReturnValue = null;
            }
            parameters.clear();
            parameters.add(km);
            currentSyscallPCB = callingPCB;
            currentCall = CallType.SendMessage;
            startTheKernel();
        }
        waitForReturnValue(callingPCB);
    }

    public static KernelMessage WaitForMessage() {
        KernelMessage msg;
        do {
            PCB callingPCB;
            synchronized (OS.class) {
                if (ki == null || ki.scheduler == null) return null;
                callingPCB = ki.scheduler.getCurrentlyRunning();
                if (callingPCB == null) return null;
                synchronized (callingPCB) {
                    callingPCB.syscallReturnValue = null;
                }
                parameters.clear();
                currentSyscallPCB = callingPCB;
                currentCall = CallType.WaitForMessage;
                startTheKernel();
            }
            msg = (KernelMessage) waitForReturnValue(callingPCB);
        } while (msg == null);
        return msg;
    }

    // ---------------- Paging Syscalls ----------------

    public static void GetMapping(int virtualPage) {
        PCB callingPCB = PCB.getCurrent();
        if (callingPCB == null) {
           
            return;
        }
        
        synchronized (OS.class) {
            if (ki == null || ki.scheduler == null) {
                System.out.println("No kernel/scheduler!");
                return;
            }
            
            
            synchronized (callingPCB) {
                callingPCB.syscallReturnValue = null;
            }
            
            parameters.clear();
            parameters.add(virtualPage);
            currentSyscallPCB = callingPCB;
            currentCall = CallType.GetMapping;
            startTheKernel();
        }
        
        // CRITICAL: Wait for the kernel to process the GetMapping
        // The kernel will set syscallReturnValue when done
        waitForReturnValue(callingPCB);
    }
    
    public static int AllocateMemory(int size) {
        PCB callingPCB = PCB.getCurrent();  // Use thread-local PCB
        synchronized (OS.class) {
            if (ki == null || ki.scheduler == null) return -1;
            if (callingPCB == null) return -1;
            
            synchronized (callingPCB) {
                callingPCB.syscallReturnValue = null;
            }
            
            parameters.clear();
            parameters.add(size);
            currentSyscallPCB = callingPCB;
            currentCall = CallType.AllocateMemory;
            startTheKernel();
        }
        
        Object result = waitForReturnValue(callingPCB);
        return (result instanceof Integer) ? (int) result : -1;
    }
    
    public static boolean FreeMemory(int pointer, int size) {
        PCB callingPCB = PCB.getCurrent();  // Use thread-local PCB
        synchronized (OS.class) {
            if (ki == null || ki.scheduler == null) return false;
            if (callingPCB == null) return false;
            
            synchronized (callingPCB) {
                callingPCB.syscallReturnValue = null;
            }
            
            parameters.clear();
            parameters.add(pointer);
            parameters.add(size);
            currentSyscallPCB = callingPCB;
            currentCall = CallType.FreeMemory;
            startTheKernel();
        }
        
        Object result = waitForReturnValue(callingPCB);
        return (result instanceof Boolean) ? (boolean) result : false;
    }
}