public interface Device {
    int Open(String deviceSpec);
    void Close(int id);
    byte[] Read(int id, int size);
    void Seek(int id, int to);
    int Write(int id, byte[] data);
}