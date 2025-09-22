public class PidExitProcess extends UserlandProcess {
    @Override
    public void main() {
        int myPid = OS.GetPID();
        System.out.println("PID_EXIT: My PID is " + myPid);
        System.out.println("PID_EXIT: I will now exit.");
        OS.Exit(); // Terminate this process.
        // This line should never be reached.
        System.out.println("PID_EXIT: ERROR! This should not be printed.");
    }
}
