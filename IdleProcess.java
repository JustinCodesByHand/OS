public class IdleProcess extends UserlandProcess {
    @Override
    public void main() {
        System.out.println("Idle process started");

        while (true) {
            System.out.println("Idle process running...");
            cooperate();

            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}