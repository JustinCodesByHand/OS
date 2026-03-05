public abstract class UserlandProcess extends Process {
    private PCB pcb;

    public UserlandProcess() {
        super();
    }
    
    public void setPCB(PCB pcb) {
        this.pcb = pcb;
    }
    
    public PCB getPCB() {
        return pcb;
    }
}