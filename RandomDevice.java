import java.util.Random;

public class RandomDevice implements Device {
    private Random[] randomDevices = new Random[10];

    @Override
    public int Open(String deviceSpec) {
        for (int i = 0; i < randomDevices.length; i++) {
            if (randomDevices[i] == null) {
                if (deviceSpec != null && !deviceSpec.isEmpty()) {
                    try {
                        long seed = Long.parseLong(deviceSpec);
                        randomDevices[i] = new Random(seed);
                    } catch (NumberFormatException e) {
                        randomDevices[i] = new Random();
                    }
                } else {
                    randomDevices[i] = new Random();
                }
                return i;
            }
        }
        return -1;
    }

    @Override
    public void Close(int id) {
        if (id >= 0 && id < randomDevices.length) randomDevices[id] = null;
    }

    @Override
    public byte[] Read(int id, int size) {
        if (id >= 0 && id < randomDevices.length && randomDevices[id] != null) {
            byte[] data = new byte[size];
            randomDevices[id].nextBytes(data);
            return data;
        }
        return null;
    }

    @Override
    public void Seek(int id, int to) { /* no-op */ }

    @Override
    public int Write(int id, byte[] data) { return 0; }
}
