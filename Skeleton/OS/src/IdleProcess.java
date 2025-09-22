public class IdleProcess extends UserlandProcess {

    @Override
    public void main() {
        System.out.println("Idle process started");

        while (true) {
            // yield control
            cooperate();
            try {
                // pause briefly
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Idle process interrupted");
                break;
            }
        }
    }
}