// DemoterProcess.java
public class DemoterProcess extends UserlandProcess {
    @Override
    public void main() {
        System.out.println("DEMOTER (Real-time): I will hog the CPU to test demotion.");
        while (true) {
            cooperate(); // This will be called after the timer forces a quantum expiration.
        }
    }
}