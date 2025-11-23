public class Main {

    public static void main(String[] args) {
        System.out.println("=== Operating System Simulator Starting ===");
        System.out.println("=== Virtual Memory Assignment Test ===\n");

        // Start with Init process only
        OS.Startup(new Init());

        System.out.println("=== System Started ===\n");
    }
}