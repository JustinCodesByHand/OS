import java.util.Random;

public class RandomDevice implements Device {

    // Array to hold up to 10 independent Random generators
    // Each slot represents one "open connection" to the random device
    // Null means that slot is available
    private Random[] randomDevices = new Random[10];

    @Override
    public int Open(String deviceSpec) {
        // Loop through all 10 slots looking for an empty one
        for (int i = 0; i < randomDevices.length; i++) {

        
            if (randomDevices[i] == null) {

                // Check if the user provided a seed value
                if (deviceSpec != null && !deviceSpec.isEmpty()) {
                    try {
                   
                        long seed = Long.parseLong(deviceSpec);

                        // Create a seeded Random to produce the same sequence
                        randomDevices[i] = new Random(seed);

                    } catch (NumberFormatException e) {
                        // If the string isn't valid, just create unseeded Random
                        randomDevices[i] = new Random();
                    }
                } else {
                    // No seed provided, make unseeded Random
                    randomDevices[i] = new Random();
                }
                // Return the slot number as the device ID
                return i;
            }
        }
        // All slots are full
        return -1;
    }

    @Override
    public void Close(int id) {
        // Check that the ID is valid, 0-9
        if (id >= 0 && id < randomDevices.length) {
            // Mark this slot as available by setting to null
            randomDevices[id] = null;
        }
    }

    @Override
    public byte[] Read(int id, int size) {
        // Validate the ID and make sure that slot is available
        if (id >= 0 && id < randomDevices.length && randomDevices[id] != null) {

            // Create a new byte array to hold the random data
            byte[] data = new byte[size];

            // Fill the array with random bytes
            // .nextBytes advances the inside index of the Random generator
            randomDevices[id].nextBytes(data);

            // Return the data to the caller
            return data;
        }
        // Invalid ID or closed device, return null = error
        return null;
    }


    @Override
    public void Seek(int id, int to) {
        // check the ID and check if the device is open
        if (id >= 0 && id < randomDevices.length && randomDevices[id] != null) {

            // Make a temp array to hold bytes
            byte[] tempArray = new byte[to];
            randomDevices[id].nextBytes(tempArray);
        }
    }

    @Override
    public int Write(int id, byte[] data) {
        return 0;
    }
}