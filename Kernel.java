// Kernel.java
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

        // Dispatcher for system calls 
        switch (call) {

            case CreateProcess: {
                UserlandProcess up = (UserlandProcess) params.get(0);
                OS.PriorityType p = (OS.PriorityType) params.get(1);
                int pid = scheduler.CreateProcess(up, p);

                // The OS simulator keeps track of the "idle" process for fallback use
                try {
                    if (scheduler.realtimeQueue != null) {
                        for (PCB pcb : new ArrayList<>(scheduler.realtimeQueue)) {
                            if (pcb.getPid() == pid) { idlePCB = pcb; break; }
                        }
                    }
                    if (idlePCB == null && scheduler.interactiveQueue != null) {
                        for (PCB pcb : new ArrayList<>(scheduler.interactiveQueue)) {
                            if (pcb.getPid() == pid) { idlePCB = pcb; break; }
                        }
                    }
                    if (idlePCB == null && scheduler.backgroundQueue != null) {
                        for (PCB pcb : new ArrayList<>(scheduler.backgroundQueue)) {
                            if (pcb.getPid() == pid) { idlePCB = pcb; break; }
                        }
                    }
                } catch (Throwable t) { }

                synchronized (OS.class) {
                    OS.retVal = pid;

                    OS.class.notifyAll();
                }
                break;
            }

            case SwitchProcess: {
                if (scheduler.currentlyRunning != null) {
                    scheduler.currentlyRunning.resetTimeoutCount();
                    scheduler.addBack(scheduler.currentlyRunning);
                    scheduler.currentlyRunning = null;
                }
                break;
            }

            case SwitchProcessQuantum: {
                if (scheduler.currentlyRunning != null) {
                    scheduler.currentlyRunning.incTimeoutCount();
                    scheduler.currentlyRunning.demoteIfNeeded(); 
                    scheduler.addBack(scheduler.currentlyRunning);
                    scheduler.currentlyRunning = null;
                }
                break;
            }

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
                    synchronized (callingPCB) {
                        int[] deviceIds = callingPCB.getDeviceIds();
                        for (int i = 0; i < deviceIds.length; i++) {
                            if (deviceIds[i] != -1) {
                                vfs.Close(deviceIds[i]);
                                deviceIds[i] = -1;
                            }
                        }
                        callingPCB.syscallReturnValue = null;
                        callingPCB.notifyAll();
                    }
                    callingPCB.stop();
                    if (scheduler.currentlyRunning == callingPCB) {
                        scheduler.currentlyRunning = null;
                    }
                    scheduler.pidToPcb.remove(callingPCB.getPid());
                    if (scheduler.waitingForMessage.containsKey(callingPCB.getPid())) {
                        scheduler.waitingForMessage.remove(callingPCB.getPid());
                    }
                }
                break;

            case Open: {
                String s = (String) params.get(0);
                int result = -1;
                if (callingPCB != null) {
                    synchronized (callingPCB) {
                        int[] deviceIds = callingPCB.getDeviceIds();
                        for (int i = 0; i < deviceIds.length; i++) {
                            if (deviceIds[i] == -1) {
                                int vfsSlot = vfs.Open(s);
                                if (vfsSlot != -1) {
                                    deviceIds[i] = vfsSlot;
                                    result = i;
                                }
                                break;
                            }
                        }
                        callingPCB.syscallReturnValue = result;
                        callingPCB.notifyAll();
                    }
                } else {
                    synchronized (OS.class) {
                        OS.retVal = -1;
                        OS.class.notifyAll();
                    }
                }
                break;
            }

            case Close: {
                int id = (int) params.get(0);
                if (callingPCB != null) {
                    synchronized (callingPCB) {
                        int[] deviceIds = callingPCB.getDeviceIds();
                        if (id >= 0 && id < deviceIds.length && deviceIds[id] != -1) {
                            vfs.Close(deviceIds[id]);
                            deviceIds[id] = -1;
                        }
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
                        int[] deviceIds = callingPCB.getDeviceIds();
                        if (id >= 0 && id < deviceIds.length && deviceIds[id] != -1) {
                            result = vfs.Read(deviceIds[id], size);
                        }
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
                    synchronized (callingPCB) {
                        int[] deviceIds = callingPCB.getDeviceIds();
                        if (id >= 0 && id < deviceIds.length && deviceIds[id] != -1) {
                            vfs.Seek(deviceIds[id], to);
                        }
                        callingPCB.syscallReturnValue = 0;
                        callingPCB.notifyAll();
                    }
                }
                break;
            }

            case GetPidByName: {
                String name = (String) params.get(0);
                int pid = -1;
                for (PCB pcb : scheduler.pidToPcb.values()) {
                    if (pcb.getName().equals(name)) {
                        pid = pcb.getPid();
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
                KernelMessage km = (KernelMessage) params.get(0);
                if (km.getTargetPid() < 0) {
                    synchronized (callingPCB) {
                        callingPCB.syscallReturnValue = -1;
                        callingPCB.notifyAll();
                    }
                    break;
                }
                KernelMessage copy = new KernelMessage(km);
                copy.setSenderPid(callingPCB.getPid());
                int targetPid = copy.getTargetPid();
                PCB target = scheduler.pidToPcb.get(targetPid);
                if (target != null) {
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
                    msg = callingPCB.messageQueue.removeFirst();
                    synchronized (callingPCB) {
                        callingPCB.syscallReturnValue = msg;
                        callingPCB.notifyAll();
                    }
                } else {
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