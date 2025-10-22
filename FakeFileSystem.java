import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;

public class FakeFileSystem implements Device {
    // This map connects our simulated "file descriptor" integers to actual RandomAccessFile handles.
    // It lets multiple files be open at once, just like real OS file tables.
    private final Map<Integer, RandomAccessFile> fdMap = new HashMap<>();
    private int nextFd = 1; 

    @Override
    public synchronized int Open(String deviceSpec) {
        if (deviceSpec == null || deviceSpec.trim().isEmpty()) return -1;
        try {
            File f = new File(deviceSpec);
            File parent = f.getAbsoluteFile().getParentFile();
            // make sure directories exist before opening, otherwise FileNotFoundException
            if (parent != null && !parent.exists()) parent.mkdirs();

            // open in read/write mode ("rw") — ensures file is created if missing
            RandomAccessFile raf = new RandomAccessFile(f, "rw");

            // each opened file gets a unique descriptor id; store it in fdMap
            int fd = nextFd++;
            fdMap.put(fd, raf);
            return fd;
        } catch (FileNotFoundException e) {
            return -1;
        } catch (SecurityException se) {
            // happens if OS denies file write permission — we just fail gracefully
            return -1;
        }
    }

    @Override
    public synchronized void Close(int id) {
        RandomAccessFile raf = fdMap.remove(id);
        if (raf != null) {
            try {
                // Before closing, explicitly flush file buffers to disk. to ensure safe commit of in-memory writes
                              
                raf.getFD().sync();
                raf.close();
            } catch (IOException e) { System.out.println("close error"); }
        }
    }

    @Override
    public synchronized byte[] Read(int id, int size) {
        RandomAccessFile raf = fdMap.get(id);
        if (raf == null) return null;       // invalid descriptor
        if (size <= 0) return new byte[0];  
        try {
            byte[] buffer = new byte[size];
            int n = raf.read(buffer);
            // read() returns -1 at EOF
            if (n < 0) return new byte[0];
            // if fewer bytes read than requested, resize array to fit actual bytes
            if (n < size) {
                byte[] actual = new byte[n];
                System.arraycopy(buffer, 0, actual, 0, n);
                return actual;
            }
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
            // Moves the file pointer to a specific byte offset.
            raf.seek(to);
        } catch (IOException e) { System.out.println("seek error");}
    }

    @Override
    public synchronized int Write(int id, byte[] data) {
        RandomAccessFile raf = fdMap.get(id);
        if (raf == null) return 0;
        if (data == null || data.length == 0) return 0;
        try {
            // write() pushes bytes into the file’s internal buffer
            raf.write(data);

            // sync() forces OS-level flush of file descriptor buffers to physical disk.
            // It’s crucial in this simulation because multiple processes may "share" the same virtual file device
    
            raf.getFD().sync();
            return data.length;
        } catch (IOException e) {
            return 0;
        }
    }
}
