
public class InteractiveProcess extends UserlandProcess {
    @Override
    public void main() {
        System.out.println("INTERACTIVE: An interactive process is running.");
        while (true) {
            System.out.println("Interactive task running...");
            cooperate();
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}