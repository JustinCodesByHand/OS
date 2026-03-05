public class KernelMessage {
    private int senderPid;
    private int targetPid;
    private int what;
    private byte[] data;

    public KernelMessage(int targetPid, int what, byte[] data) {
        this.targetPid = targetPid;
        this.what = what;
        if (data == null) {
            this.data = null;
        } else {
            this.data = data.clone();
        }
    }

    // Copy constructor
    public KernelMessage(KernelMessage other) {
        this.senderPid = other.senderPid;
        this.targetPid = other.targetPid;
        this.what = other.what;
        if (data == null) {
            this.data = null;
        } else {
            this.data = other.data.clone();
        }
    }

    public void setSenderPid(int senderPid) {
        this.senderPid = senderPid;
    }

    public int getSenderPid() {
        return senderPid;
    }

    public int getTargetPid() {
        return targetPid;
    }

    public int getWhat() {
        return what;
    }

    public byte[] getData() {
        return data != null ? data.clone() : null;
    }

    @Override
    public String toString() {
        String dataStr = (data != null) ? new String(data) : "null";
        return "from: " + senderPid + " to: " + targetPid + " what: " + what + " data: " + dataStr;
    }
}