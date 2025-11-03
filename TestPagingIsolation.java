public class TestPagingIsolation extends UserlandProcess {
    private int processNumber;
    
    public TestPagingIsolation(int num) {
        this.processNumber = num;
    }
    
    @Override
    public void main() {
        System.out.println("TestPagingIsolation-" + processNumber + " started");
        
        // Each process allocates the same virtual address
        int addr = OS.AllocateMemory(1024);
        
        if (addr >= 0) {
            System.out.println("Process-" + processNumber + ": Allocated at virtual address " + addr);
            
            // Write process-specific value
            byte myValue = (byte) (processNumber * 10);
            Hardware.Write(addr, myValue);
            Hardware.Write(addr + 100, myValue);
            Hardware.Write(addr + 500, myValue);
            
            System.out.println("Process-" + processNumber + ": Wrote value " + myValue);
            
            // Wait a bit to let other processes run
            cooperate();
            OS.Sleep(100);
            
            // Read back and verify our data is still intact
            byte val1 = Hardware.Read(addr);
            byte val2 = Hardware.Read(addr + 100);
            byte val3 = Hardware.Read(addr + 500);
            
            System.out.println("Process-" + processNumber + ": Read back " + val1 + ", " + val2 + ", " + val3);
            
            if (val1 == myValue && val2 == myValue && val3 == myValue) {
                System.out.println("Process-" + processNumber + ": SUCCESS - Memory is isolated!");
            } else {
                System.out.println("Process-" + processNumber + ": FAIL - Memory was corrupted!");
            }
            
            // Free memory
            OS.FreeMemory(addr, 1024);
            System.out.println("Process-" + processNumber + ": Freed memory");
        } else {
            System.out.println("Process-" + processNumber + ": FAIL - Could not allocate memory");
        }
        
        System.out.println("TestPagingIsolation-" + processNumber + " finished");
        OS.Exit();
    }
}