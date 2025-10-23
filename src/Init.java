// Init.java
public class Init extends UserlandProcess {

    @Override
    public void main() {
        System.out.println("Init process, making processes");

        int pid1 = OS.CreateProcess(new HelloWorld(), OS.PriorityType.realtime);
        System.out.println("Created: HelloWorld-" + pid1);

        int pid2 = OS.CreateProcess(new GoodbyeWorld(), OS.PriorityType.interactive);
        System.out.println("Created: GoodbyeWorld-" + pid2);

        int pid3 = OS.CreateProcess(new TestRandomDevice(), OS.PriorityType.interactive);
        System.out.println("Created: TestRandomDevice-" + pid3);

        int pid4 = OS.CreateProcess(new TestFileDevice(), OS.PriorityType.interactive);
        System.out.println("Created: TestFileDevice-" + pid4);

        int pid5 = OS.CreateProcess(new TestMultipleDevices(), OS.PriorityType.background);
        System.out.println("Created: TestMultipleDevices-" + pid5);

        int pidPing = OS.CreateProcess(new Ping(), OS.PriorityType.interactive);
        System.out.println("Created: Ping-" + pidPing);

        int pidPong = OS.CreateProcess(new Pong(), OS.PriorityType.interactive);
        System.out.println("Created: Pong-" + pidPong);

        System.out.println("System initialization complete");
        System.out.println("Init process in standby");

        while (true) {
            OS.switchProcess();
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}