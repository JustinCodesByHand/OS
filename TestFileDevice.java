public class TestFileDevice extends UserlandProcess {
    @Override
    public void main() {
        try {
            System.out.println("TestFileDevice process started");
            
            int id = OS.Open("file testfile.txt");
            
            if (id >= 0) {
                System.out.println("Successfully opened file device 'testfile.txt', id=" + id);

                String data = "Hello, World!";
                byte[] writeData = data.getBytes();
                int bytesWritten = OS.Write(id, writeData);
                
                System.out.println("Wrote " + bytesWritten + " bytes to file: " + data);
                
                OS.Close(id);
                System.out.println("Closed file device");
            }
            
            System.out.println("TestFileDevice process finished");
        } catch (Exception e) {
            System.err.println("TestFileDevice error: " + e.getMessage());
            e.printStackTrace();
        }
        OS.Exit();
    }
}