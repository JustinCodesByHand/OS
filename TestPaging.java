public class TestPaging extends UserlandProcess {
    
    @Override
    public void main() {
        System.out.println("\n=== TestPaging Process Started ===");
        
        // Test 1: Basic allocation
        System.out.println("\n--- Test 1: Basic Allocation ---");
        int addr1 = OS.AllocateMemory(1024); // 1 page
        if (addr1 >= 0) {
            System.out.println("SUCCESS: Allocated 1 page at virtual address " + addr1);
        } else {
            System.out.println("FAIL: Could not allocate 1 page");
        }
        
        // Test 2: Write and read
        System.out.println("\n--- Test 2: Write and Read ---");
        if (addr1 >= 0) {
            try {
                // Write some data
                Hardware.Write(addr1, (byte) 42);
                Hardware.Write(addr1 + 1, (byte) 100);
                Hardware.Write(addr1 + 500, (byte) -5);
                
                // Read it back
                byte val1 = Hardware.Read(addr1);
                byte val2 = Hardware.Read(addr1 + 1);
                byte val3 = Hardware.Read(addr1 + 500);
                
                System.out.println("Wrote: 42, 100, -5");
                System.out.println("Read:  " + val1 + ", " + val2 + ", " + val3);
                
                if (val1 == 42 && val2 == 100 && val3 == -5) {
                    System.out.println("SUCCESS: Data matches!");
                } else {
                    System.out.println("FAIL: Data mismatch!");
                }
            } catch (Exception e) {
                System.out.println("FAIL: Exception during read/write: " + e.getMessage());
            }
        }
        
        // Test 3: Multiple allocations
        System.out.println("\n--- Test 3: Multiple Allocations ---");
        int addr2 = OS.AllocateMemory(2048); // 2 pages
        int addr3 = OS.AllocateMemory(3072); // 3 pages
        
        if (addr2 >= 0 && addr3 >= 0) {
            System.out.println("SUCCESS: Allocated multiple blocks");
            System.out.println("  Block 1: " + addr1 + " (1 page)");
            System.out.println("  Block 2: " + addr2 + " (2 pages)");
            System.out.println("  Block 3: " + addr3 + " (3 pages)");
            
            // Write to each block to verify they're separate
            Hardware.Write(addr1, (byte) 1);
            Hardware.Write(addr2, (byte) 2);
            Hardware.Write(addr3, (byte) 3);
            
            byte v1 = Hardware.Read(addr1);
            byte v2 = Hardware.Read(addr2);
            byte v3 = Hardware.Read(addr3);
            
            if (v1 == 1 && v2 == 2 && v3 == 3) {
                System.out.println("SUCCESS: Memory blocks are isolated");
            } else {
                System.out.println("FAIL: Memory blocks overlap");
            }
        } else {
            System.out.println("FAIL: Could not allocate multiple blocks");
        }
        
        // Test 4: Free and reallocate
        System.out.println("\n--- Test 4: Free and Reallocate ---");
        if (addr2 >= 0) {
            boolean freed = OS.FreeMemory(addr2, 2048);
            if (freed) {
                System.out.println("SUCCESS: Freed block 2");
                
                // Try to allocate in the freed space
                int addr4 = OS.AllocateMemory(2048);
                if (addr4 >= 0) {
                    System.out.println("SUCCESS: Reallocated freed space at " + addr4);
                } else {
                    System.out.println("WARNING: Could not reallocate (might be fragmented)");
                }
            } else {
                System.out.println("FAIL: Could not free memory");
            }
        }
        
        // Test 5: Test invalid access (should cause seg fault in a separate process)
        System.out.println("\n--- Test 5: Boundary Test ---");
        try {
            // Try to access the last byte of our first allocation
            Hardware.Write(addr1 + 1023, (byte) 99);
            byte val = Hardware.Read(addr1 + 1023);
            if (val == 99) {
                System.out.println("SUCCESS: Can access last byte of allocated page");
            }
        } catch (Exception e) {
            System.out.println("FAIL: Cannot access valid memory: " + e.getMessage());
        }
        
        System.out.println("\n=== TestPaging Process Complete ===\n");
        OS.Exit();
    }
}