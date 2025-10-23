
public class TestFileDevice extends UserlandProcess {

    @Override
    public void main() {
        System.out.println("TestFileDevice process started");

        // Open a file for writing
        int id = OS.Open("testfile.txt");
        if (id >= 0) {
            System.out.println("Opened file 'testfile.txt', id=" + id);

            // Write some data
            String message = "Hello, World!";
            byte[] data = message.getBytes();
            int written = OS.Write(id, data);
            System.out.println("Wrote " + written + " bytes: " + message);

            // Close the file
            OS.Close(id);
            System.out.println("Closed file");
        }

        System.out.println("TestFileDevice finished");
        OS.Exit();
    }
}