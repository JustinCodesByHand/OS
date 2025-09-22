public class GoodbyeWorld extends UserlandProcess {
    private int messageCount = 0;

    @Override
    public void main() {
        System.out.println("GoodbyeWorld process started");

        while (true) {
            messageCount++;
            System.out.println("Goodbye World (" + messageCount + ")");

            // yield control to the scheduler if the time quantum has expired
            cooperate();

            try {
                // pause for 50ms to slow down the output
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("GoodbyeWorld process interrupted");
                break;
            }
        }
    }
}