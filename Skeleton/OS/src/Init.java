// Init.java
public class Init extends UserlandProcess {
    @Override
    public void main() {
        System.out.println("Init: Starting test processes for Priority Scheduler.");

        // 1. A real-time process that will be demoted.
        OS.CreateProcess(new DemoterProcess(), OS.PriorityType.realtime);

        // 2. A real-time process that sleeps and should NOT be demoted.
        OS.CreateProcess(new SleeperProcess(), OS.PriorityType.realtime);

        // 3. A standard interactive process.
        OS.CreateProcess(new InteractiveProcess(), OS.PriorityType.interactive);

        // 4. A process to test GetPID and Exit.
        OS.CreateProcess(new PidExitProcess(), OS.PriorityType.interactive);

        // 5. The original HelloWorld and GoodbyeWorld as background processes.
        OS.CreateProcess(new HelloWorld(), OS.PriorityType.background);
        OS.CreateProcess(new GoodbyeWorld(), OS.PriorityType.background);

        System.out.println("Init: All test processes created. Exiting.");
        OS.Exit(); // Init's job is done.
    }
}