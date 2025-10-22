public class GoodbyeWorld extends UserlandProcess {
    private int messageCount = 0;

    @Override
    public void main() {
        System.out.println("GoodbyeWorld process started");

        while (true) {
            messageCount++;
            System.out.println("Goodbye World (" + messageCount + ")");
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