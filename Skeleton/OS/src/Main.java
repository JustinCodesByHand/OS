
public class Main {

    public static void main(String[] args) {
        System.out.println("=== Operating System Simulator Starting ===");
        System.out.println("Booting with Init process...");

        // start the OS by making Init
        OS.Startup(new Init());

        System.out.println("=== Operating System Simulator Started ===");
    }
}