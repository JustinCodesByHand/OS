public class TestRandomDevice extends UserlandProcess {
    @Override
    public void main() {
        System.out.println("TestRandomDevice process started");
        
        int id = OS.Open("random 12345");
        if (id >= 0) {
            System.out.println("Successfully opened random device with seed 12345, id=" + id);
            
            byte[] randomData = OS.Read(id, 10);
            if (randomData != null) {
                System.out.print("Random bytes read: ");
                for (byte b : randomData) {
                    System.out.print((b & 0xFF) + " ");
                }
                System.out.println();
            }
            
            OS.Seek(id, 5);
            System.out.println("Seeked forward 5 positions");
            
            randomData = OS.Read(id, 5);
            if (randomData != null) {
                System.out.print("Random bytes after seek: ");
                for (byte b : randomData) {
                    System.out.print((b & 0xFF) + " ");
                }
                System.out.println();
            }
            
            OS.Close(id);
            System.out.println("Closed random device");
        } else {
            System.out.println("Failed to open random device");
        }
        
        id = OS.Open("random");
        if (id >= 0) {
            System.out.println("Successfully opened random device without seed, id=" + id);
            
            byte[] randomData = OS.Read(id, 8);
            if (randomData != null) {
                System.out.print("Random bytes (no seed): ");
                for (byte b : randomData) {
                    System.out.print((b & 0xFF) + " ");
                }
                System.out.println();
            }
            
            OS.Close(id);
            System.out.println("Closed random device");
        }
        
        System.out.println("TestRandomDevice process finished");
        OS.Exit();
    }
}