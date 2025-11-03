import java.util.ArrayList;
import java.util.List;

public class Kernel extends Process implements Device {
    public Scheduler scheduler;
    private VFS vfs = new VFS();
    private PCB idlePCB;
    
    // Free page tracking: true = in use, false = free
    // We have 1024 physical pages
    private boolean[] freePages = new boolean[1024];

    public Kernel(Scheduler scheduler) {
        this.scheduler = scheduler;
        // Initialize all pages as free
        for (int i = 0; i < 1024; i++) {
            freePages[i] = false;
        }
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

                    // Free all memory allocated to this process
                    FreeProcessMemory(callingPCB);

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

                if (callingPCB != null) {
                    synchronized (callingPCB) {
                        int[] deviceIds = callingPCB.getDeviceIds();
                        if (id >= 0 && id < deviceIds.length && deviceIds[id] != -1) {
                            int vfsSlot = deviceIds[id];
                            result = vfs.Write(vfsSlot, data);
                       
                        } else {
                            System.out.println("Invalid device id or not open");
                        }
                        callingPCB.syscallReturnValue = result;
                        callingPCB.notifyAll();
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

            case AllocateMemory: {
                int size = (int) params.get(0);
                int result = AllocateMemoryInternal(callingPCB, size);
                synchronized (callingPCB) {
                    callingPCB.syscallReturnValue = result;
                    callingPCB.notifyAll();
                }
                break;
            }

            case FreeMemory: {
                int pointer = (int) params.get(0);
                int size = (int) params.get(1);
                boolean result = FreeMemoryInternal(callingPCB, pointer, size);
                synchronized (callingPCB) {
                    callingPCB.syscallReturnValue = result;
                    callingPCB.notifyAll();
                }
                break;
            }

            case GetMapping: {
                int virtualPage = (int) params.get(0);
                GetMappingInternal(callingPCB, virtualPage);
                
                // Notify the calling process that GetMapping is complete
                if (callingPCB != null) {
                    synchronized (callingPCB) {
                        callingPCB.syscallReturnValue = 0; // Dummy value to indicate completion
                        callingPCB.notifyAll();
                    }
                }
                break;
            }

            case None:
            default:
                break;
        }
    }

    // ---------------- Memory Management Methods ----------------

    private int AllocateMemoryInternal(PCB pcb, int size) {
        if (pcb == null) return -1;
        
        if (size % 1024 != 0) {
            System.out.println("AllocateMemory: size must be multiple of 1024");
            return -1;
        }
        
        int numPages = size / 1024;
        if (numPages <= 0 || numPages > 100) {
            System.out.println("AllocateMemory: invalid number of pages: " + numPages);
            return -1;
        }
        
        int[] pageTable = pcb.getPageTable();
        int startVirtualPage = -1;
        
        for (int i = 0; i <= pageTable.length - numPages; i++) {
            boolean foundBlock = true;
            for (int j = 0; j < numPages; j++) {
                if (pageTable[i + j] != -1) {
                    foundBlock = false;
                    break;
                }
            }
            if (foundBlock) {
                startVirtualPage = i;
                break;
            }
        }
        
        if (startVirtualPage == -1) {
            System.out.println("AllocateMemory: no contiguous virtual space found");
            return -1;
        }
        
        int[] physicalPages = new int[numPages];
        int foundPages = 0;
        
        for (int i = 0; i < freePages.length && foundPages < numPages; i++) {
            if (!freePages[i]) {
                physicalPages[foundPages++] = i;
            }
        }
        
        if (foundPages < numPages) {
            System.out.println("AllocateMemory: not enough physical memory");
            return -1;
        }
        
        for (int i = 0; i < numPages; i++) {
            int physPage = physicalPages[i];
            freePages[physPage] = true;
            pageTable[startVirtualPage + i] = physPage;
        }
        
        System.out.println("AllocateMemory: allocated " + numPages + " pages starting at virtual page " + startVirtualPage);
        
        return startVirtualPage * 1024;
    }
    
    private boolean FreeMemoryInternal(PCB pcb, int pointer, int size) {
        if (pcb == null) return false;
        
        if (pointer % 1024 != 0 || size % 1024 != 0) {
            System.out.println("FreeMemory: pointer and size must be multiples of 1024");
            return false;
        }
        
        int startVirtualPage = pointer / 1024;
        int numPages = size / 1024;
        
        if (startVirtualPage < 0 || startVirtualPage + numPages > 100) {
            System.out.println("FreeMemory: invalid virtual page range");
            return false;
        }
        
        int[] pageTable = pcb.getPageTable();
        
        for (int i = 0; i < numPages; i++) {
            if (pageTable[startVirtualPage + i] == -1) {
                System.out.println("FreeMemory: trying to free unallocated page");
                return false;
            }
        }
        
        for (int i = 0; i < numPages; i++) {
            int physPage = pageTable[startVirtualPage + i];
            freePages[physPage] = false;
            pageTable[startVirtualPage + i] = -1;
        }
        
        System.out.println("FreeMemory: freed " + numPages + " pages starting at virtual page " + startVirtualPage);
        return true;
    }
    
    private void GetMappingInternal(PCB pcb, int virtualPage) {
        if (pcb == null) {
            System.out.println("GetMapping: ERROR - null PCB!");
            return;
        }
        
        int[] pageTable = pcb.getPageTable();
        
        if (virtualPage < 0 || virtualPage >= pageTable.length) {
            System.out.println("SEG FAULT: Process " + pcb.getName() + " accessed out-of-bounds virtual page " + virtualPage);
            KillProcess(pcb);
            return;
        }
        
        int physicalPage = pageTable[virtualPage];
        
        if (physicalPage == -1) {
            System.out.println("SEG FAULT: Process " + pcb.getName() + " accessed unmapped virtual page " + virtualPage);
            KillProcess(pcb);
            return;
        }
        
        // Valid mapping - update TLB
        Hardware.UpdateTLB(virtualPage, physicalPage);
    }
    
    private void FreeProcessMemory(PCB pcb) {
        if (pcb == null) return;
        
        int[] pageTable = pcb.getPageTable();
        int freedPages = 0;
        
        for (int i = 0; i < pageTable.length; i++) {
            if (pageTable[i] != -1) {
                freePages[pageTable[i]] = false;
                pageTable[i] = -1;
                freedPages++;
            }
        }
        
        if (freedPages > 0) {
            System.out.println("Freed " + freedPages + " pages from process " + pcb.getName());
        }
    }
    
    private void KillProcess(PCB pcb) {
        if (pcb == null) return;
        
        System.out.println("KERNEL: Killing process " + pcb.getName() + " due to segmentation fault");
        
        // Close all devices
        int[] deviceIds = pcb.getDeviceIds();
        for (int i = 0; i < deviceIds.length; i++) {
            if (deviceIds[i] != -1) {
                vfs.Close(deviceIds[i]);
                deviceIds[i] = -1;
            }
        }
        
        // Free memory
        FreeProcessMemory(pcb);
        
        // Request stop on the process thread
        pcb.requestStop();
        
        // Stop the process
        pcb.stop();
        
        // Remove from currently running
        if (scheduler.currentlyRunning == pcb) {
            scheduler.currentlyRunning = null;
        }
        
        // Remove from PID map
        scheduler.pidToPcb.remove(pcb.getPid());
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