public class VFS implements Device {
    // Virtual File System: manages logical connections to multiple device types.
    private static final int MAX_DEVICES = 10; 
    private Device[] devices = new Device[MAX_DEVICES];  // maps VFS slot -> actual device object
    private int[] deviceIds = new int[MAX_DEVICES];      // maps VFS slot -> device’s own internal ID

    private RandomDevice randomDevice = new RandomDevice();
    private FakeFileSystem fakeFileSystem = new FakeFileSystem();

    public VFS() {
        // initialize all slots as empty (-1 = unused)
        for (int i = 0; i < MAX_DEVICES; i++) {
            devices[i] = null;
            deviceIds[i] = -1;
        }
    }

    @Override
    public int Open(String deviceSpec) {
        if (deviceSpec == null) return -1;
        String trimmed = deviceSpec.trim();
        if (trimmed.isEmpty()) return -1;

        String deviceName;
        String params;

        // Device spec is in form like:
        //   "random 12345"  -> open random device with seed 12345
        //   "file test.txt" -> open file named test.txt
        //   "test.txt"      -> implicit "file" device
        int sp = trimmed.indexOf(' ');
        if (sp == -1) {
            // no space means no explicit device name — infer type
            if (trimmed.equalsIgnoreCase("random")) {
                deviceName = "random";
                params = "";
            } else if (trimmed.equalsIgnoreCase("file")) {
                // invalid, needs a filename after 'file'
                return -1;
            } else {
                // default assumption: it's a file path
                deviceName = "file";
                params = trimmed;
            }
        } else {
            // split the name and its parameters
            deviceName = trimmed.substring(0, sp);
            params = trimmed.substring(sp + 1).trim();
        }

        // Identify which device driver to call
        Device targetDevice;
        if ("random".equalsIgnoreCase(deviceName)) {
            targetDevice = randomDevice;
        } else if ("file".equalsIgnoreCase(deviceName)) {
            targetDevice = fakeFileSystem;
        } else {
            // unknown device name
            return -1;
        }

        
        // Each device driver  maintains its own internal IDs
        // The kernel only sees the logical "VFS slot" index.
        // So: targetDevice.Open(params) returns a device-specific handle (underlyingId),
        int underlyingId = targetDevice.Open(params);
        if (underlyingId == -1) return -1; // device refused to open (e.g., invalid file)

        // Store this open connection in the first free VFS slot
        for (int i = 0; i < MAX_DEVICES; i++) {
            if (devices[i] == null) {
                devices[i] = targetDevice;   
                deviceIds[i] = underlyingId; 
                return i;                    
            }
        }

        // if no slot was free, close it immediately 
        targetDevice.Close(underlyingId);
        return -1;
    }

    @Override
    public void Close(int id) {
        // Verify slot is valid and in use
        if (id >= 0 && id < deviceIds.length && devices[id] != null) {
            int underlying = deviceIds[id];  
            devices[id].Close(underlying);   
            devices[id] = null;              
            deviceIds[id] = -1;
        }
    }

    @Override
    public byte[] Read(int id, int size) {
        // VFS reads simply forward to the underlying device
        if (id >= 0 && id < deviceIds.length && devices[id] != null) {
            int underlying = deviceIds[id];
            return devices[id].Read(underlying, size);
        }
        return null;
    }

    @Override
    public void Seek(int id, int to) {
        if (id >= 0 && id < deviceIds.length && devices[id] != null) {
            int underlying = deviceIds[id];
            devices[id].Seek(underlying, to);
        }
    }

    @Override
    public int Write(int id, byte[] data) {
        if (id >= 0 && id < deviceIds.length && devices[id] != null) {
            int underlying = deviceIds[id];
            return devices[id].Write(underlying, data);
        }
        return 0;
    }
}
