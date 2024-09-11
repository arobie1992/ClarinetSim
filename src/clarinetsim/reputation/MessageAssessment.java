package clarinetsim.reputation;

public class MessageAssessment {

    public record ID(long nodeId, String connectionId, int seqNo) {}
    private final ID id;
    private Status status = Status.NONE;

    public MessageAssessment(ID id) {
        this.id = id;
    }

    public ID getId() {
        return id;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = Status.max(this.status, status);
    }

    public enum Status {
        NONE(0), REWARD(1), WEAK_PENALTY(2), STRONG_PENALTY(3);

        private final int value;
        Status(int value) {
            this.value = value;
        }

        private static Status max(Status s1, Status s2) {
            return s1.value > s2.value ? s1 : s2;
        }
    }

    public String toString() {
        return "{ connectionId: + \"" + id.connectionId + "\", seqNo: " + id.seqNo + ", status: " + status + "}";
    }
}
