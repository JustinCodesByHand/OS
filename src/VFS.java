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
    public int Open(String deviceSpec) {
        // Simple parsing: default to file unless starts with "random"
        Device targetDevice = fakeFileSystem;
        String params = deviceSpec;

        if (deviceSpec.startsWith("random")) {
            targetDevice = randomDevice;
            params = deviceSpec.length() > 6 ? deviceSpec.substring(7) : "";
        } else if (deviceSpec.startsWith("file ")) {
            params = deviceSpec.substring(5);
        }

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

        // No slots available
        targetDevice.Close(underlyingId);
        return -1;
    }

    @Override
    public void Close(int id) {
        if (id >= 0 && id < deviceIds.length && devices[id] != null) {
            devices[id].Close(deviceIds[id]);
            devices[id] = null;
            deviceIds[id] = -1;
        }
    }

    @Override
    public byte[] Read(int id, int size) {
        if (id >= 0 && id < deviceIds.length && devices[id] != null) {
            return devices[id].Read(deviceIds[id], size);
        }
        return null;
    }

    @Override
    public void Seek(int id, int to) {
        if (id >= 0 && id < deviceIds.length && devices[id] != null) {
            devices[id].Seek(deviceIds[id], to);
        }
    }

    @Override
    public int Write(int id, byte[] data) {
        if (id >= 0 && id < deviceIds.length && devices[id] != null) {
            return devices[id].Write(deviceIds[id], data);
        }
        return 0;
    }
}