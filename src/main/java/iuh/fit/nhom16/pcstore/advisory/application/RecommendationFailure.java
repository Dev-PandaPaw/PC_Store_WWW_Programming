package iuh.fit.nhom16.pcstore.advisory.application;

public class RecommendationFailure extends RuntimeException {
    public final int status;
    public final String code;
    public RecommendationFailure(int status, String code, String message) {
        super(message); this.status = status; this.code = code;
    }
}
