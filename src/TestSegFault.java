public class TestSegFault extends UserlandProcess {
    
    @Override
    public void main() {
        System.out.println("\n=== TestSegFault Process Started ===");
        System.out.println("This process will intentionally cause a segmentation fault");
        
        // Allocate 1 page
        int addr = OS.AllocateMemory(1024);
        
        if (addr >= 0) {
            System.out.println("Allocated 1 page at address " + addr);
            
            // Valid access
            System.out.println("Attempting valid write...");
            Hardware.Write(addr, (byte) 42);
            System.out.println("SUCCESS: Valid write completed");
            
            // Wait a moment
            cooperate();
            
            // Invalid access - try to access memory we didn't allocate
            System.out.println("Attempting INVALID write to unmapped page...");
            try {
                // Try to access virtual page 50 (which we didn't allocate)
                Hardware.Write(50 * 1024, (byte) 99);
                System.out.println("ERROR: Should have caused seg fault!");
            } catch (Exception e) {
                System.out.println("EXPECTED: Caught exception - " + e.getMessage());
            }
        }
        
        System.out.println("If you see this, the process wasn't killed (unexpected)");
        OS.Exit();
    }
}