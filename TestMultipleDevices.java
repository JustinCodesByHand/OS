public class TestMultipleDevices extends UserlandProcess {
    @Override
    public void main() {
        System.out.println("TestMultipleDevices process started");

        int randomId1 = OS.Open("random 111");
        int randomId2 = OS.Open("random 222");
        if (randomId1 >= 0 && randomId2 >= 0) {
            System.out.println("Opened two random devices: id1=" + randomId1 + ", id2=" + randomId2);

            byte[] data1 = OS.Read(randomId1, 5);
            byte[] data2 = OS.Read(randomId2, 5);
            if (data1 != null && data2 != null) {
                System.out.print("Random1: ");
                for (byte b : data1) System.out.print((b & 0xFF) + " ");
                System.out.println();
                System.out.print("Random2: ");
                for (byte b : data2) System.out.print((b & 0xFF) + " ");
                System.out.println();
            }

            OS.Close(randomId1);
            OS.Close(randomId2);
            System.out.println("Closed random devices");
        }

        int fileId = OS.Open("file sharedfile.txt");
        if (fileId >= 0) {
            System.out.println("Opened file device 'sharedfile.txt', id=" + fileId);

            String msg = "Shared data";
            OS.Write(fileId, msg.getBytes());
            System.out.println("Wrote to shared file: " + msg);

            OS.Seek(fileId, 0);
            byte[] readData = OS.Read(fileId, 20);
            if (readData != null) {
                System.out.println("Read from shared file: '" + new String(readData) + "'");
            }

            OS.Close(fileId);
            System.out.println("Closed file device");
        }

        System.out.println("Note: Multiple processes can connect to the same device (e.g., same file) as each gets its own handle");

        System.out.println("TestMultipleDevices process finished");
        OS.Exit();
    }
}