package iuh.fit.nhom16.pcstore.ai;

public class AiFailure extends RuntimeException {
    public final int status;
    public final String code;
    public final int retryAfterSeconds;

    public AiFailure(int status, String code, String message) { this(status, code, message, 0); }
    public AiFailure(int status, String code, String message, int retryAfterSeconds) {
        super(message);
        this.status = status;
        this.code = code;
        this.retryAfterSeconds = retryAfterSeconds;
    }
}
