public class Hardware {
    // Physical memory: 1MB = 1024 pages * 1024 bytes/page
    private static final int PAGE_SIZE = 1024;
    private static final int NUM_PAGES = 1024;
    private static final int MEMORY_SIZE = PAGE_SIZE * NUM_PAGES; // 1MB
    
    // Physical memory array
    private static byte[] memory = new byte[MEMORY_SIZE];
    
    // TLB: 2 entries, each holding [virtualPage, physicalPage]
    private static int[][] tlb = new int[2][2];
    
    static {
        // Initialize TLB to invalid values
        ClearTLB();
    }
    
    /**
     * Clear the TLB by setting all entries to -1
     * Called on process switch
     */
    public static void ClearTLB() {
        for (int i = 0; i < 2; i++) {
            tlb[i][0] = -1;  // virtual page
            tlb[i][1] = -1;  // physical page
        }
    }
    
    /**
     * Read a byte from virtual memory address
     */
    public static byte Read(int virtualAddress) {
        int virtualPage = GetVirtualPage(virtualAddress);
        int pageOffset = GetPageOffset(virtualAddress);
        
        // Check TLB first
        int physicalPage = LookupTLB(virtualPage);
        
        if (physicalPage == -1) {
            // TLB miss, need to get mapping from OS
            OS.GetMapping(virtualPage);
            
            // Try TLB again, should be there now
            physicalPage = LookupTLB(virtualPage);
            
            if (physicalPage == -1) {
                // Still not in TLB, process was killed for seg fault
                throw new RuntimeException("Process terminated - segmentation fault on read");
            }
        }
        
        // Calculate physical address and read
        int physicalAddress = GetPhysicalAddress(physicalPage, pageOffset);
        return memory[physicalAddress];
    }
    
    /**
     * Write a byte to virtual memory address
     */
    public static void Write(int virtualAddress, byte value) {
        int virtualPage = GetVirtualPage(virtualAddress);
        int pageOffset = GetPageOffset(virtualAddress);
        
        // Check TLB first
        int physicalPage = LookupTLB(virtualPage);
        
        if (physicalPage == -1) {
            // TLB miss, need to get mapping from OS
            OS.GetMapping(virtualPage);
            
            // Try TLB again, should be there now
            physicalPage = LookupTLB(virtualPage);
            
            if (physicalPage == -1) {
                // Still not in TLB,  process was killed for seg fault
                throw new RuntimeException("Process terminated - segmentation fault on write");
            }
        }
        
        // Calculate physical address and write
        int physicalAddress = GetPhysicalAddress(physicalPage, pageOffset);
        memory[physicalAddress] = value;
    }
    
    /**
     * Helper: Get virtual page number from virtual address
     */
    private static int GetVirtualPage(int virtualAddress) {
        return virtualAddress / PAGE_SIZE;
    }
    
    /**
     * Helper: Get offset within page from virtual address
     */
    private static int GetPageOffset(int virtualAddress) {
        return virtualAddress % PAGE_SIZE;
    }
    
    /**
     * Helper: Calculate physical address from physical page and offset
     */
    private static int GetPhysicalAddress(int physicalPage, int pageOffset) {
        return physicalPage * PAGE_SIZE + pageOffset;
    }
    
    /**
     * Helper: Look up virtual page in TLB
     * Returns physical page number if found, -1 if not found
     */
    private static int LookupTLB(int virtualPage) {
        for (int i = 0; i < 2; i++) {
            if (tlb[i][0] == virtualPage) {
                return tlb[i][1];
            }
        }
        return -1;
    }
    
    /**
     * Update TLB with new mapping
     * Called by OS.GetMapping() 
     * Uses random replacement
     */
    public static void UpdateTLB(int virtualPage, int physicalPage) {
        // Random replacement, pick entry 0 or 1
        int entry = (int)(Math.random() * 2);
        tlb[entry][0] = virtualPage;
        tlb[entry][1] = physicalPage;
    }
    
    /**
     * NEW: Allow kernel to access physical memory directly for swapping
     */
    public static byte[] getMemory() {
        return memory;
    }
}