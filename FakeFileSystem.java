import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;

public class FakeFileSystem implements Device {
    private final Map<Integer, RandomAccessFile> fdMap = new HashMap<>();
    private int nextFd = 1; 

    @Override
    public synchronized int Open(String deviceString) {
        if (deviceString == null || deviceString.trim().isEmpty()) {
            System.out.println("FakeFileSystem.Open: empty filename");
            return -1;
        }
        
        String filename = deviceString.trim();
        System.out.println("FakeFileSystem.Open: opening '" + filename + "'");
        
        try {
            File f = new File(filename);
            RandomAccessFile raf = new RandomAccessFile(f, "rw");
            int fd = nextFd++;
            fdMap.put(fd, raf);
            System.out.println("FakeFileSystem.Open: success, fd=" + fd);
            return fd;
        } catch (FileNotFoundException e) {
            System.out.println("FakeFileSystem.Open: failed - " + e.getMessage());
            return -1;
        }
    }

    @Override
    public synchronized void Close(int id) {
        RandomAccessFile raf = fdMap.remove(id);
        if (raf != null) {
            try {
                raf.close();
                System.out.println("FakeFileSystem.Close: closed fd=" + id);
            } catch (IOException e) { 
                System.out.println("FakeFileSystem.Close: error closing fd=" + id);
            }
        }
    }

    @Override
    public synchronized byte[] Read(int id, int size) {
        RandomAccessFile raf = fdMap.get(id);

        if (raf == null) {
            System.out.println("FakeFileSystem.Read: invalid fd=" + id);
            return null;
        }
        if (size <= 0) {
            System.out.println("FakeFileSystem.Read: invalid size=" + size);
            return null;
        }

        try {
            byte[] buffer = new byte[size];
            int bytesRead = raf.read(buffer);
            System.out.println("FakeFileSystem.Read: fd=" + id + ", requested=" + size + ", read=" + bytesRead);
            
            if (bytesRead == -1) {
                return new byte[0];
            } else if (bytesRead < size) {
                byte[] trimmed = new byte[bytesRead];
                System.arraycopy(buffer, 0, trimmed, 0, bytesRead);
                return trimmed;
            }
            
            return buffer;
        } catch (IOException e) {
            System.out.println("FakeFileSystem.Read: IO error - " + e.getMessage());
            return null;
        }
    }

    @Override
    public synchronized void Seek(int id, int to) {
        RandomAccessFile raf = fdMap.get(id);
        if (raf == null) {
            System.out.println("FakeFileSystem.Seek: invalid fd=" + id);
            return;
        }
        try {
            raf.seek(to);
            System.out.println("FakeFileSystem.Seek: fd=" + id + ", position=" + to);
        } catch (IOException e) { 
            System.out.println("FakeFileSystem.Seek: error - " + e.getMessage());
        }
    }

    @Override
    public synchronized int Write(int id, byte[] data) {
        RandomAccessFile raf = fdMap.get(id);
        if (raf == null) {
            System.out.println("FakeFileSystem.Write: invalid fd=" + id);
            return 0;
        }
        if (data == null || data.length == 0) {
            System.out.println("FakeFileSystem.Write: no data to write");
            return 0;
        }
        try {
            raf.write(data);
            System.out.println("FakeFileSystem.Write: fd=" + id + ", wrote " + data.length + " bytes");
            return data.length;
        } catch (IOException e) {
            System.out.println("FakeFileSystem.Write: IO error - " + e.getMessage());
            return 0;
        }
    }
}