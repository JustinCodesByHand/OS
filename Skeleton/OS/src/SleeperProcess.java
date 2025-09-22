public class SleeperProcess extends UserlandProcess {
    @Override
    public void main() {
        System.out.println("SLEEPER (Real-time): I will sleep often to avoid demotion.");
        while (true) {
            System.out.println("Sleeper: Woke up, doing some work...");
            cooperate();
            System.out.println("Sleeper: Going to sleep for 300ms.");
            OS.Sleep(300); // Sleep for a duration longer than the quantum.
        }
    }
}