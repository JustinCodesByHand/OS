public class TestPaging extends UserlandProcess {
    
    @Override
    public void main() {
        System.out.println("\n=== TestPaging Process Started ===");
        
        try {
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
            
            // Test 3: Multiple allocations (but only test 2 at a time due to TLB size)
            System.out.println("\n--- Test 3: Multiple Allocations ---");
            int addr2 = OS.AllocateMemory(2048); // 2 pages
            
            if (addr2 >= 0) {
                System.out.println("SUCCESS: Allocated 2 pages at address " + addr2);
                
                try {
                    // Test first two allocations only (TLB has 2 entries)
                    Hardware.Write(addr1, (byte) 1);
                    Hardware.Write(addr2, (byte) 2);
                    
                    byte v1 = Hardware.Read(addr1);
                    byte v2 = Hardware.Read(addr2);
                    
                    if (v1 == 1 && v2 == 2) {
                        System.out.println("SUCCESS: Memory blocks are isolated");
                    } else {
                        System.out.println("FAIL: Got v1=" + v1 + ", v2=" + v2);
                    }
                } catch (Exception e) {
                    System.out.println("FAIL: Exception during isolation test: " + e.getMessage());
                }
            } else {
                System.out.println("FAIL: Could not allocate second block");
            }
            
            // Test 4: Free and reallocate
            System.out.println("\n--- Test 4: Free and Reallocate ---");
            if (addr2 >= 0) {
                boolean freed = OS.FreeMemory(addr2, 2048);
                if (freed) {
                    System.out.println("SUCCESS: Freed block 2");
                    
                    // Try to allocate in the freed space
                    int addr3 = OS.AllocateMemory(2048);
                    if (addr3 >= 0) {
                        System.out.println("SUCCESS: Reallocated freed space at " + addr3);
                        
                        // Verify we can use it
                        try {
                            Hardware.Write(addr3, (byte) 99);
                            byte v = Hardware.Read(addr3);
                            if (v == 99) {
                                System.out.println("SUCCESS: New allocation is usable");
                            }
                        } catch (Exception e) {
                            System.out.println("FAIL: Could not use reallocated memory");
                        }
                    } else {
                        System.out.println("WARNING: Could not reallocate");
                    }
                } else {
                    System.out.println("FAIL: Could not free memory");
                }
            }
            
            System.out.println("\n=== TestPaging Process Complete ===\n");
        } catch (Exception e) {
            System.out.println("\nTestPaging CRASHED: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("TestPaging: Calling OS.Exit()...");
        System.out.flush();
        OS.Exit();
        System.out.println("TestPaging: After OS.Exit() - THIS SHOULD NOT PRINT");
        System.out.flush();
    }
}