public class HelloWorld extends UserlandProcess {
    private int messageCount = 0;

    @Override
    public void main() {
        System.out.println("HelloWorld process started");

        while (true) {
            messageCount++;
            System.out.println("Hello World (" + messageCount + ")");
            cooperate();

            try {
                Thread.sleep(20); // slow for readability
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}