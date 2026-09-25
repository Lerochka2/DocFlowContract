package by.bsuir.docflow.docflow_backend.dto.auth;

public class PasswordCodeVerificationResponse {

    private final String changeToken;
    private final long expiresInSeconds;


    public PasswordCodeVerificationResponse(
            String changeToken,
            long expiresInSeconds
    ) {
        this.changeToken =
                changeToken;

        this.expiresInSeconds =
                expiresInSeconds;
    }


    public String getChangeToken() {
        return changeToken;
    }


    public long getExpiresInSeconds() {
        return expiresInSeconds;
    }
}