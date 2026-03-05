public class VFS implements Device {
    private static final int MAX_DEVICES = 10;
    private Device[] devices = new Device[MAX_DEVICES];
    private int[] deviceIds = new int[MAX_DEVICES];

    private RandomDevice randomDevice = new RandomDevice();
    private FakeFileSystem fakeFileSystem = new FakeFileSystem();

    public VFS() {
        // Initialize as empty
        for (int i = 0; i < MAX_DEVICES; i++) {
            devices[i] = null;
            deviceIds[i] = -1;
        }
    }

    @Override
    public synchronized int Open(String deviceSpec) {
        if (deviceSpec == null || deviceSpec.isEmpty()) {
            return -1;
        }

        // Parse device specification
        Device targetDevice = fakeFileSystem;
        String params = deviceSpec;

        if (deviceSpec.startsWith("random")) {
            targetDevice = randomDevice;
            // Extract seed if provided (format: "random 12345" or just "random")
            params = deviceSpec.length() > 6 ? deviceSpec.substring(7).trim() : "";
        } else if (deviceSpec.startsWith("file ")) {
            // Explicit file prefix (format: "file myfile.txt")
            targetDevice = fakeFileSystem;
            params = deviceSpec.substring(5).trim();
        }
        // If no prefix, default to file with full deviceSpec as filename

        // Open device
        int underlyingId = targetDevice.Open(params);
        if (underlyingId == -1) return -1;

        // Find empty VFS slot
        for (int i = 0; i < MAX_DEVICES; i++) {
            if (devices[i] == null) {
                devices[i] = targetDevice;
                deviceIds[i] = underlyingId;
                return i;
            }
        }

        // No slots available - clean up
        targetDevice.Close(underlyingId);
        return -1;
    }

    @Override
    public synchronized void Close(int id) {
        if (id >= 0 && id < deviceIds.length && devices[id] != null) {
            devices[id].Close(deviceIds[id]);
            devices[id] = null;
            deviceIds[id] = -1;
        }
    }

    @Override
    public synchronized byte[] Read(int id, int size) {
        if (id >= 0 && id < deviceIds.length && devices[id] != null) {
            return devices[id].Read(deviceIds[id], size);
        }
        return null;
    }

    @Override
    public synchronized void Seek(int id, int to) {
        if (id >= 0 && id < deviceIds.length && devices[id] != null) {
            devices[id].Seek(deviceIds[id], to);
        }
    }

    @Override
    public synchronized int Write(int id, byte[] data) {
        if (id >= 0 && id < deviceIds.length && devices[id] != null) {
            int result = devices[id].Write(deviceIds[id], data);
            return result;
        }
       
        return 0;
    }
}