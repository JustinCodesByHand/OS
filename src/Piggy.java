public class Piggy extends UserlandProcess {
    private static int instanceCounter = 0;
    private int instanceId;
    
    public Piggy() {
        super();
        synchronized (Piggy.class) {
            instanceId = ++instanceCounter;
        }
    }
    
    @Override
    public void main() {
        int myPid = -1;
        try {
            myPid = OS.GetPID();
            System.out.println("===== Piggy-" + myPid + " START (instance " + instanceId + ") =====");
            
            // Allocate 50 pages = 50KB (allows more processes to test swapping better)
            int addr = OS.AllocateMemory(50 * 1024);
            
            if (addr == -1) {
                System.out.println("Piggy-" + myPid + ": FAILED to allocate");
                OS.Exit();
                return;
            }
            
            System.out.println("Piggy-" + myPid + ": Allocated 50 pages at address " + addr);
            
            // PHASE 1: Write unique pattern to ALL pages
            System.out.println("Piggy-" + myPid + ": PHASE 1 - Writing unique data to 50 pages...");
            for (int i = 0; i < 50; i++) {
                int writeAddr = addr + (i * 1024);
                byte value = (byte) (instanceId * 10 + i);
                Hardware.Write(writeAddr, value);
                
                // Write to multiple locations in each page to be thorough
                Hardware.Write(writeAddr + 100, (byte)(value + 1));
                Hardware.Write(writeAddr + 500, (byte)(value + 2));
                Hardware.Write(writeAddr + 900, (byte)(value + 3));
            }
            System.out.println("Piggy-" + myPid + ": Finished writing all pages");
            
            // PHASE 2: Sleep to allow other processes to run
            // This gives other processes time to allocate memory and force swapping
            System.out.println("Piggy-" + myPid + ": PHASE 2 - Sleeping (2s) to allow other processes to trigger swapping...");
            OS.Sleep(2000);
            
            // PHASE 3: Verify ALL data is still correct (tests if swapping preserved our data)
            System.out.println("Piggy-" + myPid + ": PHASE 3 - Verifying data integrity after potential swap...");
            boolean success = true;
            int errors = 0;
            
            for (int i = 0; i < 50; i++) {
                int readAddr = addr + (i * 1024);
                byte expectedBase = (byte) (instanceId * 10 + i);
                
                byte actual1 = Hardware.Read(readAddr);
                byte actual2 = Hardware.Read(readAddr + 100);
                byte actual3 = Hardware.Read(readAddr + 500);
                byte actual4 = Hardware.Read(readAddr + 900);
                
                byte expected1 = expectedBase;
                byte expected2 = (byte)(expectedBase + 1);
                byte expected3 = (byte)(expectedBase + 2);
                byte expected4 = (byte)(expectedBase + 3);
                
                if (actual1 != expected1 || actual2 != expected2 || 
                    actual3 != expected3 || actual4 != expected4) {
                    
                    if (errors < 3) {
                        System.out.println("Piggy-" + myPid + ": ERROR at page " + i);
                        System.out.println("  Expected: " + (expected1 & 0xFF) + ", " + 
                                         (expected2 & 0xFF) + ", " + 
                                         (expected3 & 0xFF) + ", " + 
                                         (expected4 & 0xFF));
                        System.out.println("  Got:      " + (actual1 & 0xFF) + ", " + 
                                         (actual2 & 0xFF) + ", " + 
                                         (actual3 & 0xFF) + ", " + 
                                         (actual4 & 0xFF));
                    }
                    errors++;
                    success = false;
                }
            }
            
            if (success) {
                System.out.println("Piggy-" + myPid + ": ✓ SUCCESS - All data verified! (Swapping worked correctly)");
            } else {
                System.out.println("Piggy-" + myPid + ": ✗ FAILED - " + errors + " pages corrupted");
            }
            
            // PHASE 4: Do another round to stress test
            System.out.println("Piggy-" + myPid + ": PHASE 4 - Second verification pass...");
            OS.Sleep(1000);
            
            int secondPassErrors = 0;
            for (int i = 0; i < 25; i++) {  // Check first 25 pages
                int readAddr = addr + (i * 1024);
                byte expected = (byte) (instanceId * 10 + i);
                byte actual = Hardware.Read(readAddr);
                
                if (actual != expected) {
                    secondPassErrors++;
                }
            }
            
            if (secondPassErrors == 0) {
                System.out.println("Piggy-" + myPid + ": ✓ Second pass SUCCESS!");
            } else {
                System.out.println("Piggy-" + myPid + ": ✗ Second pass found " + secondPassErrors + " errors");
            }
            
            OS.FreeMemory(addr, 50 * 1024);
            System.out.println("===== Piggy-" + myPid + " END =====\n");
            
        } catch (Exception e) {
            System.out.println("Piggy-" + myPid + ": CRASHED: " + e.getMessage());
            e.printStackTrace();
        } finally {
            OS.Exit();
        }
    }
}