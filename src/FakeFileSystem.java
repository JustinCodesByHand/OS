import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;

public class FakeFileSystem implements Device {
    // This map connects the "file descriptor" integers to actual RandomAccessFile
    // It lets multiple files be open at once
    private final Map<Integer, RandomAccessFile> fdMap = new HashMap<>();
    private int nextFd = 1; 

    @Override
    public synchronized int Open(String deviceString) {
        if (deviceString == null || deviceString.trim().isEmpty()) return -1;
        try {
            File f = new File(deviceString);

            // open in read/write mode ("rw"), ensures file is created if missing
            RandomAccessFile raf = new RandomAccessFile(f, "rw");

            // each opened file gets a unique descriptor id; store it in fdMap
            int fd = nextFd++;
            fdMap.put(fd, raf);
            return fd;
        } catch (FileNotFoundException e) {
            return -1;
        }
    }

    @Override
    public synchronized void Close(int id) {
        // Remove the file descriptor from the map
        // remove() returns the associated RandomAccessFile
        RandomAccessFile raf = fdMap.remove(id);
        if (raf != null) {
            try {
                raf.close();
            } catch (IOException e) { System.out.println("close error"); }
        }
    }

    @Override
    public synchronized byte[] Read(int id, int size) {
        // Look up the file handle for this descriptor
        RandomAccessFile raf = fdMap.get(id);

        // Check for invalid descriptor or bad size
        if (raf == null || size <= 0) return null;

        try {
            byte[] buffer = new byte[size];

            // Read from file into buffer
            raf.read(buffer);
            return buffer;
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public synchronized void Seek(int id, int to) {
        RandomAccessFile raf = fdMap.get(id);
        if (raf == null) return;
        try {
            // Moves the file pointer to a specific byte offset
            raf.seek(to);
        } catch (IOException e) { System.out.println("seek error");}
    }

    @Override
    public synchronized int Write(int id, byte[] data) {
        RandomAccessFile raf = fdMap.get(id);
        if (raf == null) return 0;
        if (data == null || data.length == 0) return 0;
        try {
            // .write() pushes bytes into the file’s internal buffer
            raf.write(data);

            return data.length;
        } catch (IOException e) {
            return 0;
        }
    }
}
