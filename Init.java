// Init.java
public class Init extends UserlandProcess {

    @Override
    public void main() {
        System.out.println("Init process, making processes");

        // Create basic processes
        int pid1 = OS.CreateProcess(new HelloWorld(), OS.PriorityType.realtime);
        System.out.println("Created: HelloWorld-" + pid1);

        int pid2 = OS.CreateProcess(new GoodbyeWorld(), OS.PriorityType.interactive);
        System.out.println("Created: GoodbyeWorld-" + pid2);

        // Wait a bit for them to run
        OS.Sleep(300);

        // Create file system test
        System.out.println("\n----- TESTING FILE SYSTEM -------\n");
        int pid4 = OS.CreateProcess(new TestFileDevice(), OS.PriorityType.interactive);
        System.out.println("Created: TestFileDevice-" + pid4);

        // Wait for file test to complete
        OS.Sleep(2000);

        // Create SIMPLE paging test first
        System.out.println("\n------- SIMPLE PAGING TEST -------\n");
        int pidSimple = OS.CreateProcess(new SimplePagingTest(), OS.PriorityType.interactive);
        System.out.println("Created: SimplePagingTest-" + pidSimple);
        
        // Give it plenty of time to complete
        OS.Sleep(2000);

        // Create paging tests - ONE AT A TIME
       // System.out.println("\n------ FULL PAGING TEST -------\n");
        
        int pidPaging = OS.CreateProcess(new TestPaging(), OS.PriorityType.interactive);
        System.out.println("Created: TestPaging-" + pidPaging);

        // Wait for it to complete
        OS.Sleep(3000);

        // Create isolation tests - ONE AT A TIME
      //  System.out.println("\n------- TESTING PROCESS ISOLATION (Test 1) ---------\n");
        int pidIso1 = OS.CreateProcess(new TestPagingIsolation(1), OS.PriorityType.interactive);
        System.out.println("Created: TestPagingIsolation-1");
        OS.Sleep(2000);

      //  System.out.println("\n-------- TESTING PROCESS ISOLATION (Test 2) ---------\n");
        int pidIso2 = OS.CreateProcess(new TestPagingIsolation(2), OS.PriorityType.interactive);
        System.out.println("Created: TestPagingIsolation-2");
        OS.Sleep(2000);

      //  System.out.println("\n------- TESTING PROCESS ISOLATION (Test 3) --------\n");
        int pidIso3 = OS.CreateProcess(new TestPagingIsolation(3), OS.PriorityType.interactive);
        System.out.println("Created: TestPagingIsolation-3");
        OS.Sleep(2000);

        // Create seg fault test
      //  System.out.println("\n--------- TESTING SEGMENTATION FAULT ---------\n");
        int pidSegFault = OS.CreateProcess(new TestSegFault(), OS.PriorityType.interactive);
        System.out.println("Created: TestSegFault-" + pidSegFault);

        // Wait a bit
        OS.Sleep(2000);

        
        System.out.println("Init process in standby");

        while (true) {
            OS.switchProcess();
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}