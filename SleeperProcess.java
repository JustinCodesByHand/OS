public class SleeperProcess extends UserlandProcess {
    @Override
    public void main() {
        int round = 0;
        while (true) {
            round++;
            System.out.println("Sleeper round " + round + " running");
            cooperate();
            OS.Sleep(1000);
        }
    }
}