// Pong.java
public class Pong extends UserlandProcess {
    @Override
    public void main() {
        System.out.println("Pong process started");
        int pingPid = OS.GetPidByName("Ping");
        while (pingPid == -1) {
            OS.Sleep(50);  // Short sleep to avoid busy-waiting; adjust if needed
            pingPid = OS.GetPidByName("Ping");
        }
        System.out.println("I am PONG, ping = " + pingPid);

        while (true) {
            KernelMessage received = OS.WaitForMessage();
            System.out.println("  PONG: from: " + received.getSenderPid() + " to: " + received.getTargetPid() + " what: " + received.getWhat());

            KernelMessage response = new KernelMessage(pingPid, received.getWhat() + 1, null);
            OS.SendMessage(response);

            cooperate();

            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}