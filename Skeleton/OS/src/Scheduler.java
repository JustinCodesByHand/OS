import java.util.LinkedList;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class Scheduler {
    // Three new queues for each priority level, plus a list for sleeping processes.
    private final LinkedList<PCB> realTimeQueue = new LinkedList<>();
    private final LinkedList<PCB> interactiveQueue = new LinkedList<>();
    private final LinkedList<PCB> backgroundQueue = new LinkedList<>();
    private final LinkedList<PCB> sleepingProcesses = new LinkedList<>();

    private PCB currentlyRunning;
    private final Timer timer;
    private final Random random = new Random();

    public Scheduler() {
        this.timer = new Timer(true);
        this.timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (currentlyRunning != null) {
                    // Increment the timeout counter when the timer fires.
                    currentlyRunning.setTimeoutCount(currentlyRunning.getTimeoutCount() + 1);
                    currentlyRunning.requestStop();
                }
            }
        }, 250, 250);
    }

    // This method is now simpler, just adding to the correct queue.
    public int createProcess(UserlandProcess up, OS.PriorityType p) {
        PCB pcb = new PCB(up, p);
        switch (p) {
            case realtime:
                realTimeQueue.add(pcb);
                break;
            case interactive:
                interactiveQueue.add(pcb);
                break;
            case background:
                backgroundQueue.add(pcb);
                break;
        }
        return pcb.getPid();
    }

    // This is the new, updated scheduling logic.
    public PCB schedule() {
        // 1. Awaken any sleeping processes whose time is up.
        long currentTime = System.currentTimeMillis();
        var iterator = sleepingProcesses.iterator();
        while (iterator.hasNext()) {
            PCB pcb = iterator.next();
            if (pcb.getWakeupTime() <= currentTime) {
                // Add it back to the correct ready queue.
                addProcessToQueue(pcb);
                iterator.remove(); // Remove it from the sleeping list.
            }
        }

        // 2. Add the previously running process back to its queue and handle demotion.
        if (currentlyRunning != null && !currentlyRunning.isDone()) {
            // Demote if it has timed out 5 or more times in a row.
            if (currentlyRunning.getTimeoutCount() >= 5) {
                if (currentlyRunning.getPriority() == OS.PriorityType.realtime) {
                    currentlyRunning.setPriority(OS.PriorityType.interactive);
                    System.out.println("Process " + currentlyRunning.getPid() + " demoted to Interactive.");
                } else if (currentlyRunning.getPriority() == OS.PriorityType.interactive) {
                    currentlyRunning.setPriority(OS.PriorityType.background);
                    System.out.println("Process " + currentlyRunning.getPid() + " demoted to Background.");
                }
                currentlyRunning.setTimeoutCount(0); // Reset counter after demotion.
            }
            addProcessToQueue(currentlyRunning);
        }

        // 3. Probabilistic selection to choose the next process.
        int choice = random.nextInt(100); // Random number between 0-99.

        if (!realTimeQueue.isEmpty() && choice < 60) { // 60% chance for real-time
            currentlyRunning = realTimeQueue.pollFirst();
        } else if (!interactiveQueue.isEmpty() && choice < 90) { // 30% chance for interactive
            currentlyRunning = interactiveQueue.pollFirst();
        } else if (!backgroundQueue.isEmpty()) { // 10% chance for background
            currentlyRunning = backgroundQueue.pollFirst();
        } else {
            // Fallback if the chosen queue was empty but others are not.
            if (!realTimeQueue.isEmpty()) currentlyRunning = realTimeQueue.pollFirst();
            else if (!interactiveQueue.isEmpty()) currentlyRunning = interactiveQueue.pollFirst();
            else if (!backgroundQueue.isEmpty()) currentlyRunning = backgroundQueue.pollFirst();
            else currentlyRunning = null; // No processes ready to run.
        }

        return currentlyRunning;
    }

    // Helper method to add a process to the correct queue.
    private void addProcessToQueue(PCB pcb) {
        switch (pcb.getPriority()) {
            case realtime:
                realTimeQueue.add(pcb);
                break;
            case interactive:
                interactiveQueue.add(pcb);
                break;
            case background:
                backgroundQueue.add(pcb);
                break;
        }
    }

    // New methods to support the new system calls.
    public void sleep(int milliseconds) {
        if (currentlyRunning != null) {
            currentlyRunning.setWakeupTime(System.currentTimeMillis() + milliseconds);
            currentlyRunning.setTimeoutCount(0); // Calling sleep resets the timeout counter.
            sleepingProcesses.add(currentlyRunning);
            currentlyRunning = null;
        }
    }

    public void exit() {
        currentlyRunning = null; // Discard the current process.
    }
}