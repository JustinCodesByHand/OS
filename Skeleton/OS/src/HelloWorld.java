/**
 * A simple userland process that prints "Hello World" in a loop.
 */
public class HelloWorld extends UserlandProcess {
    private int messageCount = 0;

    /**
     * The main execution loop for this process.
     */
    @Override
    public void main() {
        System.out.println("HelloWorld process started");

        while (true) {
            messageCount++;
            System.out.println("Hello World (" + messageCount + ")");

            // give up control to the scheduler if the time quantum has expired
            cooperate();

            try {
                // pause for 50ms to slow down the output
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("HelloWorld process interrupted");
                break;
            }
        }
    }
}