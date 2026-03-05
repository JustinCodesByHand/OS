public class VirtualToPhysicalMapping {
    public int physicalPage;
    public int diskPage;
    
    public VirtualToPhysicalMapping() {
        this.physicalPage = -1;
        this.diskPage = -1;
    }
}