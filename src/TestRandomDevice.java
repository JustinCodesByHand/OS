
public class TestRandomDevice extends UserlandProcess {

    @Override
    public void main() {
        System.out.println("TestRandomDevice process started");

        // Test seeded random device
        int id = OS.Open("random 12345");
        if (id >= 0) {
            System.out.println("Opened random device with seed, id=" + id);

            // Read some random bytes
            byte[] data = OS.Read(id, 5);
            if (data != null) {
                System.out.print("Random bytes: ");
                for (byte b : data) {
                    System.out.print((b & 0xFF) + " ");
                }
                System.out.println();
            }

            OS.Close(id);
        }

        System.out.println("TestRandomDevice finished");
        OS.Exit();
    }
}