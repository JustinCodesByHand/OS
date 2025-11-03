public class TestFileDevice extends UserlandProcess {

    @Override
    public void main() {
        System.out.println("TestFileDevice process started");

        // Test 1: Write and read back
        int id = OS.Open("testfile.txt");
        if (id >= 0) {
            System.out.println("Opened file 'testfile.txt', id=" + id);

            // Write some data
            String message = "Hello, World!";
            byte[] data = message.getBytes();
            int written = OS.Write(id, data);
            System.out.println("Write returned: " + written + " bytes (expected " + data.length + ")");
            
            if (written == data.length) {
                System.out.println("SUCCESS: Write operation successful");
            } else {
                System.out.println("WARNING: Write returned different count than expected");
            }

            // Seek back to beginning and read
            OS.Seek(id, 0);
            byte[] readData = OS.Read(id, message.length());
            if (readData != null) {
                String readMessage = new String(readData);
                System.out.println("Read back " + readData.length + " bytes: '" + readMessage + "'");
                if (message.equals(readMessage)) {
                    System.out.println("SUCCESS: Write/Read verified!");
                } else {
                    System.out.println("ERROR: Data mismatch!");
                }
            } else {
                System.out.println("ERROR: Read returned null");
            }

            // Close the file
            OS.Close(id);
            System.out.println("Closed file");
        } else {
            System.out.println("ERROR: Failed to open file");
        }

        System.out.println("TestFileDevice finished");
        OS.Exit();
    }
}