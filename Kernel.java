import java.util.ArrayList;
import java.util.List;

public class Kernel extends Process implements Device {
    public Scheduler scheduler;
    private VFS vfs = new VFS();
    private PCB idlePCB;
    
    private boolean[] freeListofPages = new boolean[1024];
    
    private int swapFileId = -1;
    private int nextSwapPage = 0;

    public Kernel(Scheduler scheduler) {
        this.scheduler = scheduler;
        
        for (int i = 0; i < 1024; i++) {
            freeListofPages[i] = false;
        }
        
        swapFileId = vfs.Open("swapfile.dat");
        if (swapFileId == -1) {
            System.out.println("WARNING: Could not open swap file!");
        } else {
            System.out.println("Swap file opened, id=" + swapFileId);
        }
    }

    public void main() {
        while (true) {
            handleSystemCall();

            PCB next = scheduler.schedule();

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

        switch (call) {

            case CreateProcess: {
                UserlandProcess up = (UserlandProcess) params.get(0);
                OS.PriorityType p = (OS.PriorityType) params.get(1);

                int pid = scheduler.CreateProcess(up, p);

                if (up instanceof IdleProcess) {
                    if (!scheduler.backgroundQueue.isEmpty()) {
                        PCB lastAdded = scheduler.backgroundQueue.peekLast();
                        if (lastAdded != null && lastAdded.getPid() == pid) {
                            idlePCB = lastAdded;
                        }
                    }
                }

                synchronized (OS.class) {
                    OS.retVal = pid;
                    OS.class.notifyAll();
                }
                break;
            }

            case SwitchProcess:
                if (scheduler.currentlyRunning != null) {
                    scheduler.currentlyRunning.resetTimeoutCount();
                    scheduler.currentlyRunning.stop();
                    scheduler.addBack(scheduler.currentlyRunning);
                    scheduler.currentlyRunning = null;
                }
                break;

            case SwitchProcessQuantum:
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
                    int[] deviceIds = callingPCB.getDeviceIds();
                    for (int i = 0; i < deviceIds.length; i++) {
                        if (deviceIds[i] != -1) {
                            vfs.Close(deviceIds[i]);
                            deviceIds[i] = -1;
                        }
                    }

                    FreeProcessMemory(callingPCB);

                    callingPCB.stop();

                    if (scheduler.currentlyRunning == callingPCB) {
                        scheduler.currentlyRunning = null;
                    }
                    
                    // Remove from scheduler's process map
                    scheduler.pidToPcb.remove(callingPCB.getPid());
                }
                break;

            case Open: {
                String devicePath = (String) params.get(0);
                int result = -1;

                if (callingPCB != null) {
                    int[] deviceIds = callingPCB.getDeviceIds();
                    for (int i = 0; i < deviceIds.length; i++) {
                        if (deviceIds[i] == -1) {
                            int vfsSlot = vfs.Open(devicePath);
                            if (vfsSlot != -1) {
                                deviceIds[i] = vfsSlot;
                                result = i;
                                break;
                            }
                        }
                    }

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
                    int[] deviceIds = callingPCB.getDeviceIds();
                    if (id >= 0 && id < deviceIds.length && deviceIds[id] != -1) {
                        vfs.Seek(deviceIds[id], to);
                    }

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
                
                if (callingPCB != null) {
                    synchronized (callingPCB) {
                        callingPCB.syscallReturnValue = 0;
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
        if (pcb == null) {
            return -1;
        }

        if (size % 1024 != 0) {
            System.out.println("AllocateMemory: size must be multiple of 1024");
            return -1;
        }

        int numPages = size / 1024;
        if (numPages <= 0 || numPages > 100) {
            System.out.println("AllocateMemory: invalid number of pages: " + numPages);
            return -1;
        }

        VirtualToPhysicalMapping[] pageTable = pcb.getPageTable();
        int startVirtualPage = -1;

        for (int i = 0; i <= pageTable.length - numPages; i++) {
            boolean foundBlock = true;
            for (int j = 0; j < numPages; j++) {
                if (pageTable[i + j] != null) {
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
            System.out.println("AllocateMemory: no virtual space found");
            return -1;
        }

        // LAZY ALLOCATION
        for (int i = 0; i < numPages; i++) {
            pageTable[startVirtualPage + i] = new VirtualToPhysicalMapping();
        }

        System.out.println("AllocateMemory: lazily allocated " + numPages + 
                          " pages starting at virtual page " + startVirtualPage);

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

        VirtualToPhysicalMapping[] pageTable = pcb.getPageTable();

        for (int i = 0; i < numPages; i++) {
            if (pageTable[startVirtualPage + i] == null) {
                System.out.println("FreeMemory: trying to free unallocated page");
                return false;
            }
        }

        for (int i = 0; i < numPages; i++) {
            VirtualToPhysicalMapping mapping = pageTable[startVirtualPage + i];
            
            if (mapping.physicalPage != -1) {
                freeListofPages[mapping.physicalPage] = false;
            }
            
            pageTable[startVirtualPage + i] = null;
        }

        System.out.println("FreeMemory: freed " + numPages + " pages starting at virtual page "
                + startVirtualPage);
        return true;
    }
    
    private void GetMappingInternal(PCB pcb, int virtualPage) {
        if (pcb == null) {
            System.out.println("GetMapping: ERROR - null PCB!");
            return;
        }

        VirtualToPhysicalMapping[] pageTable = pcb.getPageTable();

        if (virtualPage < 0 || virtualPage >= pageTable.length) {
            System.out.println("SEG FAULT: Process " + pcb.getName() + 
                             " accessed out-of-bounds virtual page " + virtualPage);
            KillProcess(pcb);
            return;
        }

        VirtualToPhysicalMapping mapping = pageTable[virtualPage];
        if (mapping == null) {
            System.out.println("SEG FAULT: Process " + pcb.getName() + 
                             " accessed unallocated virtual page " + virtualPage);
            KillProcess(pcb);
            return;
        }

        // DEMAND PAGING
        if (mapping.physicalPage == -1) {
            System.out.println("Page fault: Process " + pcb.getName() + 
                             " accessing virtual page " + virtualPage + " for first time");
            
            // Try to find a free physical page
            int freePage = -1;
            for (int i = 0; i < freeListofPages.length; i++) {
                if (!freeListofPages[i]) {
                    freePage = i;
                    break;
                }
            }
            
            // If no free page, swap one out
            if (freePage == -1) {
                System.out.println("No free physical pages - need to swap");
                
                PCB victimPCB = scheduler.getRandomProcess();
                if (victimPCB == null) {
                    System.out.println("CRITICAL: No process to swap from!");
                    KillProcess(pcb);
                    return;
                }
                
                VirtualToPhysicalMapping[] victimTable = victimPCB.getPageTable();
                int victimVirtualPage = -1;
                for (int i = 0; i < victimTable.length; i++) {
                    if (victimTable[i] != null && victimTable[i].physicalPage != -1) {
                        victimVirtualPage = i;
                        break;
                    }
                }
                
                if (victimVirtualPage == -1) {
                    System.out.println("CRITICAL: Victim has no physical pages!");
                    KillProcess(pcb);
                    return;
                }
                
                VirtualToPhysicalMapping victimMapping = victimTable[victimVirtualPage];
                freePage = victimMapping.physicalPage;
                
                System.out.println("Swapping out: Process " + victimPCB.getName() + 
                                 ", VP " + victimVirtualPage + ", PP " + freePage);
                
                if (victimMapping.diskPage == -1) {
                    victimMapping.diskPage = nextSwapPage++;
                }
                
                WritePageToDisk(freePage, victimMapping.diskPage);
                
                victimMapping.physicalPage = -1;
                
                // IMPORTANT: Clear TLB after swapping to invalidate old mappings
                Hardware.ClearTLB();
            }
            
            // Assign physical page
            mapping.physicalPage = freePage;
            freeListofPages[freePage] = true;
            
            // Load from disk or zero out
            if (mapping.diskPage != -1) {
                System.out.println("Loading from disk: disk page " + mapping.diskPage);
                LoadPageFromDisk(mapping.diskPage, freePage);
            } else {
                System.out.println("Zeroing out new page " + freePage);
                ZeroOutPage(freePage);
            }
        }
        
        // Update TLB - this ensures current process has correct mapping
        Hardware.UpdateTLB(virtualPage, mapping.physicalPage);
    }
    
    // Direct physical memory access
    private void WritePageToDisk(int physicalPage, int diskPage) {
        if (swapFileId == -1) {
            System.out.println("ERROR: Swap file not open!");
            return;
        }
        
        byte[] pageData = new byte[1024];
        byte[] memory = Hardware.getMemory();
        int physicalAddress = physicalPage * 1024;
        
        // Copy from physical memory
        System.arraycopy(memory, physicalAddress, pageData, 0, 1024);
        
        // Write to swap file
        vfs.Seek(swapFileId, diskPage * 1024);
        int written = vfs.Write(swapFileId, pageData);
        
        if (written != 1024) {
            System.out.println("WARNING: Wrote " + written + " bytes instead of 1024");
        }
    }
    
    //Direct physical memory access
    private void LoadPageFromDisk(int diskPage, int physicalPage) {
        if (swapFileId == -1) {
            System.out.println("ERROR: Swap file not open!");
            ZeroOutPage(physicalPage);
            return;
        }
        
        vfs.Seek(swapFileId, diskPage * 1024);
        byte[] pageData = vfs.Read(swapFileId, 1024);
        
        if (pageData == null || pageData.length != 1024) {
            System.out.println("ERROR: Could not read full page from disk");
            ZeroOutPage(physicalPage);
            return;
        }
        
        // Write to physical memory
        byte[] memory = Hardware.getMemory();
        int physicalAddress = physicalPage * 1024;
        System.arraycopy(pageData, 0, memory, physicalAddress, 1024);
    }
    
    // Direct physical memory access
    private void ZeroOutPage(int physicalPage) {
        byte[] memory = Hardware.getMemory();
        int physicalAddress = physicalPage * 1024;
        
        for (int i = 0; i < 1024; i++) {
            memory[physicalAddress + i] = 0;
        }
    }
    
    private void FreeProcessMemory(PCB pcb) {
        if (pcb == null) return;

        VirtualToPhysicalMapping[] pageTable = pcb.getPageTable();
        int freedPages = 0;

        for (int i = 0; i < pageTable.length; i++) {
            if (pageTable[i] != null) {
                if (pageTable[i].physicalPage != -1) {
                    freeListofPages[pageTable[i].physicalPage] = false;
                    freedPages++;
                }
                pageTable[i] = null;
            }
        }

        if (freedPages > 0) {
            System.out.println("Freed " + freedPages + " pages from process " + pcb.getName());
        }
    }
    
    private void KillProcess(PCB pcb) {
        if (pcb == null) return;

        System.out.println("KERNEL: Killing process " + pcb.getName() + " due to segmentation fault");

        int[] deviceIds = pcb.getDeviceIds();
        for (int i = 0; i < deviceIds.length; i++) {
            if (deviceIds[i] != -1) {
                vfs.Close(deviceIds[i]);
                deviceIds[i] = -1;
            }
        }

        FreeProcessMemory(pcb);

        pcb.requestStop();
        pcb.stop();

        if (scheduler.currentlyRunning == pcb) {
            scheduler.currentlyRunning = null;
        }

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