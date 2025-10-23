import java.util.ArrayList;
import java.util.List;

public class Kernel extends Process implements Device {
    public Scheduler scheduler;
    private VFS vfs = new VFS();
    private PCB idlePCB;

    public Kernel(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public void main() {
        while (true) {
            // Continuously listen for system calls from OS/userland
            handleSystemCall();

            // Choose next process to run using the scheduler
            PCB next = scheduler.schedule();

            // If nothing is ready, fall back to idle process (keeps CPU "alive")
            if (next == null && idlePCB != null) {
                next = idlePCB;
            }
            if (next != null) {
                scheduler.currentlyRunning = next;
                next.start();
            }
            this.stop();
        }
    }

    private void handleSystemCall() {
        OS.CallType call;
        List<Object> params;
        PCB callingPCB;

        // Synchronize access to OS syscall queue since multiple processes might request syscalls
        synchronized (OS.class) {
            call = OS.currentCall;
            OS.currentCall = OS.CallType.None;
            params = new ArrayList<>(OS.parameters);
            OS.parameters.clear();
            callingPCB = OS.currentSyscallPCB;
            OS.currentSyscallPCB = null;
        }
        if (callingPCB == null) {
            callingPCB = scheduler.getCurrentlyRunning();
        }

        // Switch for system calls
        switch (call) {

            case CreateProcess: {
                // Extract parameters
                UserlandProcess up = (UserlandProcess) params.get(0);
                OS.PriorityType p = (OS.PriorityType) params.get(1);

                // Create the process
                int pid = scheduler.CreateProcess(up, p);

                // If this is the idle process, save it for fallback use
                if (up instanceof IdleProcess) {
                    // Find the PCB we just created in the background queue
                    // Note: IdleProcess is always created with background priority
                    if (!scheduler.backgroundQueue.isEmpty()) {
                        PCB lastAdded = scheduler.backgroundQueue.peekLast();
                        if (lastAdded != null && lastAdded.getPid() == pid) {
                            idlePCB = lastAdded;
                        }
                    }
                }

                // Store the PID as the return value
                synchronized (OS.class) {
                    OS.retVal = pid;
                    OS.class.notifyAll();
                }
                break;
            }

            case SwitchProcess:
                // Process is voluntarily giving up CPU
                if (scheduler.currentlyRunning != null) {
                    scheduler.currentlyRunning.resetTimeoutCount();
                    scheduler.currentlyRunning.stop();
                    scheduler.addBack(scheduler.currentlyRunning); // put back into ready queue
                    scheduler.currentlyRunning = null;
                }
                break;

            case SwitchProcessQuantum:
                // Process used its full time quantum
                if (scheduler.currentlyRunning != null) {
                    scheduler.currentlyRunning.incTimeoutCount();
                    scheduler.currentlyRunning.demoteIfNeeded();
                    scheduler.currentlyRunning.stop();
                    scheduler.addBack(scheduler.currentlyRunning);
                    scheduler.currentlyRunning = null;
                }
                break;

            case Sleep: {
                int ms = 0;
                if (!params.isEmpty()) {
                    ms = (int) params.get(0);
                }
                if (scheduler.currentlyRunning != null) {
                    long wake = System.currentTimeMillis() + ms;
                    PCB sleepingPCB = scheduler.currentlyRunning;
                    scheduler.currentlyRunning = null;
                    scheduler.sleepProcess(sleepingPCB, wake);
                }
                break;
            }

            case GetPID:
                if (callingPCB != null) {
                    synchronized (callingPCB) {
                        callingPCB.syscallReturnValue = callingPCB.getPid();

                        callingPCB.notifyAll();
                    }
                }
                break;

            case Exit:
                if (callingPCB != null) {
                    // Close all open devices
                    int[] deviceIds = callingPCB.getDeviceIds();
                    for (int i = 0; i < deviceIds.length; i++) {
                        if (deviceIds[i] != -1) {
                            vfs.Close(deviceIds[i]);
                            deviceIds[i] = -1;
                        }
                    }

                    // Stop the process
                    callingPCB.stop();

                    // Clear from scheduler if it's currently running
                    if (scheduler.currentlyRunning == callingPCB) {
                        scheduler.currentlyRunning = null;
                    }
                }
                break;

            case Open: {
                String devicePath = (String) params.get(0);
                int result = -1;

                if (callingPCB != null) {
                    // Find empty slot in process's device array
                    int[] deviceIds = callingPCB.getDeviceIds();
                    for (int i = 0; i < deviceIds.length; i++) {
                        if (deviceIds[i] == -1) {
                            // Open device and map to user fd
                            int vfsSlot = vfs.Open(devicePath);
                            if (vfsSlot != -1) {
                                deviceIds[i] = vfsSlot;
                                result = i;
                                break;
                            }
                        }
                    }

                    // Store result and wake up process
                    synchronized (callingPCB) {
                        callingPCB.syscallReturnValue = result;
                        callingPCB.notifyAll();
                    }
                }
                break;
            }

            case Close: {
                int id = (int) params.get(0);

                if (callingPCB != null) {
                    // Close the device if valid
                    synchronized (callingPCB) {
                    int[] deviceIds = callingPCB.getDeviceIds();
                    if (id >= 0 && id < deviceIds.length && deviceIds[id] != -1) {
                        vfs.Close(deviceIds[id]);
                        deviceIds[id] = -1;
                    }

                    // Store result and wake up process

                        callingPCB.syscallReturnValue = 0;
                        callingPCB.notifyAll();
                    }

                }
                break;
            }

            case Read: {
                int id = (int) params.get(0);
                int size = (int) params.get(1);
                byte[] result = null;

                if (callingPCB != null) {
                    synchronized (callingPCB) {
                    // Read from device if valid
                    int[] deviceIds = callingPCB.getDeviceIds();
                    if (id >= 0 && id < deviceIds.length && deviceIds[id] != -1) {
                        result = vfs.Read(deviceIds[id], size);
                    }

                    // Return result

                        callingPCB.syscallReturnValue = result;
                        callingPCB.notifyAll();
                    }
                }
                break;
            }

            case Write: {
                int id = (int) params.get(0);
                byte[] data = (byte[]) params.get(1);
                int result = 0;

                PCB originalCaller = callingPCB;

                if (originalCaller != null) {
                    synchronized (originalCaller) {
                        int[] deviceIds = originalCaller.getDeviceIds();
                        if (id >= 0 && id < deviceIds.length) {
                            int vfsSlot = deviceIds[id];
                            if (vfsSlot != -1) {
                                result = vfs.Write(vfsSlot, data);
                            }
                        }
                        originalCaller.syscallReturnValue = result;
                        originalCaller.notifyAll();
                    }
                } else {
                    PCB running = scheduler.getCurrentlyRunning();
                    if (running != null) {
                        synchronized (running) {
                            int[] deviceIds = running.getDeviceIds();
                            if (id >= 0 && id < deviceIds.length) {
                                int vfsSlot = deviceIds[id];
                                if (vfsSlot != -1) {
                                    result = vfs.Write(vfsSlot, data);
                                }
                            }
                            running.syscallReturnValue = result;
                            running.notifyAll();
                        }
                    }
                }
                break;
            }

            case Seek: {
                int id = (int) params.get(0);
                int to = (int) params.get(1);

                if (callingPCB != null) {
                    // Seek in device if valid
                    int[] deviceIds = callingPCB.getDeviceIds();
                    if (id >= 0 && id < deviceIds.length && deviceIds[id] != -1) {
                        vfs.Seek(deviceIds[id], to);
                    }

                    // Store result and wake up process
                    synchronized (callingPCB) {
                        callingPCB.syscallReturnValue = 0;
                        callingPCB.notifyAll();
                    }
                }
                break;
            }
            case GetPidByName: {
                String name = (String) params.get(0); // extract target process name from syscall parameters
                int pid = -1; // default PID if process not found
                for (PCB pcb : scheduler.pidToPcb.values()) {
                    if (pcb.getName().equals(name)) {
                        pid = pcb.getPid(); // get PID of matching process
                        break;
                    }
                }
                synchronized (callingPCB) {
                    callingPCB.syscallReturnValue = pid;
                    callingPCB.notifyAll();
                }
                break;
            }

            case SendMessage: {
                KernelMessage km = (KernelMessage) params.get(0); // extract message object from parameters

                // validate target PID
                if (km.getTargetPid() < 0) {
                    synchronized (callingPCB) {
                        callingPCB.syscallReturnValue = -1;
                        callingPCB.notifyAll(); // notify calling process of failure
                    }
                    break;
                }
                KernelMessage copy = new KernelMessage(km); // create a copy to avoid shared reference
                copy.setSenderPid(callingPCB.getPid());
                int targetPid = copy.getTargetPid();
                PCB target = scheduler.pidToPcb.get(targetPid);
                if (target != null) {
                    // deliver message to target's message queue
                    target.messageQueue.add(copy);
                    if (scheduler.waitingForMessage.containsKey(targetPid)) {
                        PCB waiting = scheduler.waitingForMessage.remove(targetPid);
                        scheduler.addBack(waiting);
                    }
                }
                synchronized (callingPCB) {
                    callingPCB.syscallReturnValue = null;
                    callingPCB.notifyAll();
                }
                break;
            }

            case WaitForMessage: {
                KernelMessage msg = null;
                if (!callingPCB.messageQueue.isEmpty()) {
                    // if message is already in queue, retrieve it immediately
                    msg = callingPCB.messageQueue.removeFirst();
                    synchronized (callingPCB) {
                        callingPCB.syscallReturnValue = msg;
                        callingPCB.notifyAll();
                    }
                } else {
                    // if no message is available, block the process
                    scheduler.waitingForMessage.put(callingPCB.getPid(), callingPCB);
                    scheduler.currentlyRunning = null;
                    synchronized (callingPCB) {
                        callingPCB.syscallReturnValue = null;
                        callingPCB.notifyAll();
                    }
                }
                break;
            }


            case None:
            default:
                // no pending system call
                break;
        }
    }

    // ---------------- Device Interface Methods ----------------

    @Override
    // Open a device using a specific string ("file data.txt", "random 123")
    // Returns a device ID for future ops, or -1 if failed
    public int Open(String s) {
        PCB pcb = scheduler.getCurrentlyRunning();
        if (pcb == null) return -1;
        int[] deviceIds = pcb.getDeviceIds();
        for (int i = 0; i < deviceIds.length; i++) {
            if (deviceIds[i] == -1) {
                int vfsSlot = vfs.Open(s);
                if (vfsSlot == -1) return -1;
                deviceIds[i] = vfsSlot;
                return i;
            }
        }
        return -1;
    }

    @Override
    // Close an open device connection and free its resources
    // The device ID becomes -1 after closing
    public void Close(int id) {
        PCB pcb = scheduler.getCurrentlyRunning();
        if (pcb == null) return;
        int[] deviceIds = pcb.getDeviceIds();
        if (id >= 0 && id < deviceIds.length && deviceIds[id] != -1) {
            vfs.Close(deviceIds[id]);
            deviceIds[id] = -1;
        }
    }

    @Override
    // Read up to 'size' bytes from the device starting at current position
    // Returns byte array with data read, or null if failed
    public byte[] Read(int id, int size) {
        PCB pcb = scheduler.getCurrentlyRunning();
        if (pcb == null) return null;
        int[] deviceIds = pcb.getDeviceIds();
        if (id >= 0 && id < deviceIds.length) {
            int vfsSlot = deviceIds[id];
            if (vfsSlot != -1) {
                return vfs.Read(vfsSlot, size);
            }
        }
        return null;
    }

    @Override
    // Move the read/write position to a specific location within the device
    public void Seek(int id, int to) {
        PCB pcb = scheduler.getCurrentlyRunning();
        if (pcb == null) return;
        int[] deviceIds = pcb.getDeviceIds();
        if (id >= 0 && id < deviceIds.length) {
            int vfsSlot = deviceIds[id];
            if (vfsSlot != -1) {
                vfs.Seek(vfsSlot, to);
            }
        }
    }

    @Override
    // Write data bytes to the device at current position
    // Returns actual number of bytes written
    public int Write(int id, byte[] data) {
        PCB pcb = scheduler.getCurrentlyRunning();
        if (pcb == null) return 0;
        int[] deviceIds = pcb.getDeviceIds();
        if (id >= 0 && id < deviceIds.length) {
            int vfsSlot = deviceIds[id];
            if (vfsSlot != -1) {
                return vfs.Write(vfsSlot, data);
            }
        }
        return 0;
    }
}
