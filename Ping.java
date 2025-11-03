public class Ping extends UserlandProcess {
    @Override
    public void main() {
        System.out.println("Ping process started");
        int pongPid = OS.GetPidByName("Pong");
        while (pongPid == -1) {
            OS.Sleep(50);  // Short sleep to avoid busy-waiting; 
            pongPid = OS.GetPidByName("Pong");
        }
        System.out.println("I am PING, pong = " + pongPid);
        int what = 0;

        while (true) {
            KernelMessage km = new KernelMessage(pongPid, what, null);
            OS.SendMessage(km);

            KernelMessage received = OS.WaitForMessage();
            System.out.println("  PING: from: " + received.getSenderPid() + " to: " + received.getTargetPid() + " what: " + received.getWhat());

            what++;
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