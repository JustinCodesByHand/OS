public class Init extends UserlandProcess {

    @Override
    public void main() {
        System.out.println("Init: Starting Virtual Memory Test");
        System.out.flush();

        // Create HelloWorld and GoodbyeWorld for background activity
        System.out.println("Init: Creating background processes...");
        int hw = OS.CreateProcess(new HelloWorld(), OS.PriorityType.realtime);
        System.out.println("Created: HelloWorld-" + hw);
        
        int gw = OS.CreateProcess(new GoodbyeWorld(), OS.PriorityType.interactive);
        System.out.println("Created: GoodbyeWorld-" + gw);
        
        OS.Sleep(500);
        System.out.println();

        // Create 15 Piggy processes (each uses 80 pages = 80KB)
        // Total: 15 * 80 = 1200 pages needed, but only 1024 available
        // This WILL trigger swapping!
        
        System.out.println("Init: Creating 15 Piggy processes...");
        System.out.println("Each Piggy uses 80 pages (80KB)");
        System.out.println("Total needed: 1200 pages > 1024 available");
        System.out.println("Swapping WILL occur!\n");
        System.out.flush();
        
        for (int i = 0; i < 15; i++) {
            Piggy p = new Piggy();
            int pid = OS.CreateProcess(p, OS.PriorityType.background);
            System.out.println("Created: Piggy-" + pid + " (instance " + (i+1) + "/15)");
            System.out.flush();
        }
        
        System.out.println("\n=== All 15 Piggy processes created ===");
        System.out.println("Waiting for them to complete...\n");
        System.out.flush();
        
        // Give them time to run
        OS.Sleep(60000);
        
        System.out.println("\n=== TEST COMPLETE ===");
        System.out.flush();

        // Keep Init alive
        while (true) {
            OS.switchProcess();
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}