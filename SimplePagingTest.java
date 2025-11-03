public class SimplePagingTest extends UserlandProcess {
    
    @Override
    public void main() {
        System.out.println("\n=== SimplePagingTest Started ===");
        System.out.println("My PID: " + OS.GetPID());
        
        // Step 1: Allocate
        System.out.println("\nStep 1: Allocating 1 page (1024 bytes)");
        int addr = OS.AllocateMemory(1024);
        System.out.println("Allocated at virtual address: " + addr);
        
        if (addr < 0) {
            System.out.println("FAIL: Could not allocate memory");
            OS.Exit();
            return;
        }
        
        // Step 2: Write
        System.out.println("\nStep 2: Writing byte 42 to address " + addr);
        try {
            Hardware.Write(addr, (byte) 42);
            System.out.println("SUCCESS: Write completed");
        } catch (Exception e) {
            System.out.println("FAIL: Write failed - " + e.getMessage());
            OS.Exit();
            return;
        }
        
        // Step 3: Read
        System.out.println("\nStep 3: Reading from address " + addr);
        try {
            byte val = Hardware.Read(addr);
            System.out.println("Read value: " + val);
            if (val == 42) {
                System.out.println("SUCCESS: Value matches!");
            } else {
                System.out.println("FAIL: Value mismatch (expected 42, got " + val + ")");
            }
        } catch (Exception e) {
            System.out.println("FAIL: Read failed - " + e.getMessage());
            OS.Exit();
            return;
        }
        
        // Step 4: Free
        System.out.println("\nStep 4: Freeing memory");
        boolean freed = OS.FreeMemory(addr, 1024);
        System.out.println("Free result: " + freed);
        
        System.out.println("\n=== SimplePagingTest Complete ===\n");
        OS.Exit();
    }
}