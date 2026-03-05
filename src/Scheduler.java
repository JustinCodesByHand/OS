import java.util.*;
import java.util.concurrent.*;

public class Scheduler {

    final LinkedList<PCB> realtimeQueue;
    final LinkedList<PCB> interactiveQueue;
    final LinkedList<PCB> backgroundQueue;

    private final Map<PCB, Long> sleeping;

    public PCB currentlyRunning;

    private final Random rand;

    public HashMap<Integer, PCB> pidToPcb = new HashMap<>();

    public HashMap<Integer, PCB> waitingForMessage = new HashMap<>();

    public Scheduler() {
        realtimeQueue = new LinkedList<>();
        interactiveQueue = new LinkedList<>();
        backgroundQueue = new LinkedList<>();
        sleeping = new LinkedHashMap<>();
        currentlyRunning = null;
        rand = new Random();

        Timer quantumTimer = new Timer(true);
        quantumTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                synchronized (Scheduler.this) {
                    if (currentlyRunning != null && !currentlyRunning.isDone()) {
                        currentlyRunning.requestStop();
                    }
                }
            }
        }, 250, 250);
    }

    public synchronized int CreateProcess(UserlandProcess up, OS.PriorityType priority) {
        PCB pcb = new PCB(up, priority);
        pidToPcb.put(pcb.getPid(), pcb);
        switch (priority) {
            case realtime: realtimeQueue.addLast(pcb); break;
            case interactive: interactiveQueue.addLast(pcb); break;
            case background: backgroundQueue.addLast(pcb); break;
            default: interactiveQueue.addLast(pcb); break;
        }
        return pcb.getPid();
    }

    public synchronized void addBack(PCB pcb) {
        if (pcb == null || pcb.isDone()) return;
        switch (pcb.getPriority()) {
            case realtime: realtimeQueue.addLast(pcb); break;
            case interactive: interactiveQueue.addLast(pcb); break;
            case background: backgroundQueue.addLast(pcb); break;
            default: interactiveQueue.addLast(pcb); break;
        }
    }

    public synchronized void sleepProcess(PCB pcb, long wakeTime) {
        if (pcb == null) return;
        sleeping.put(pcb, wakeTime);
    }

    private synchronized void wakeSleeping() {
        long now = System.currentTimeMillis();
        sleeping.entrySet().removeIf(entry -> {
            if (entry.getValue() <= now) {
                addBack(entry.getKey());
                return true;
            }
            return false;
        });
    }

    public synchronized PCB getCurrentlyRunning() {
        return currentlyRunning;
    }

    public synchronized PCB schedule() {
        wakeSleeping();

        int r = rand.nextInt(10) + 1;
        LinkedList<PCB> chosen;
        if (r <= 6) chosen = realtimeQueue;
        else if (r <= 9) chosen = interactiveQueue;
        else chosen = backgroundQueue;

        if (chosen.isEmpty()) {
            if (!realtimeQueue.isEmpty()) chosen = realtimeQueue;
            else if (!interactiveQueue.isEmpty()) chosen = interactiveQueue;
            else if (!backgroundQueue.isEmpty()) chosen = backgroundQueue;
            else return null;
        }

        PCB next = chosen.pollFirst();
        
        if (next != null && currentlyRunning != next) {
            Hardware.ClearTLB();
        }
        
        currentlyRunning = next;
        return next;
    }
    
   
    public synchronized PCB getRandomProcess() {
        List<PCB> allProcesses = new ArrayList<>(pidToPcb.values());
        
        if (allProcesses.isEmpty()) {
            return null;
        }
        
        // Keep trying random processes until we find one with physical memory
        Collections.shuffle(allProcesses, rand);
        
        for (PCB pcb : allProcesses) {
            VirtualToPhysicalMapping[] pageTable = pcb.getPageTable();
            // Check if this process has at least one page with physical memory
            for (int i = 0; i < pageTable.length; i++) {
                if (pageTable[i] != null && pageTable[i].physicalPage != -1) {
                    return pcb;
                }
            }
        }
        
        return null;
    }
}