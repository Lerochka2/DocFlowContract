package by.bsuir.docflow.docflow_backend.dto.auth;

public class PasswordCodeRequestResponse {

    private final String maskedEmail;
    private final long expiresInSeconds;
    private final long resendAvailableInSeconds;


    public PasswordCodeRequestResponse(
            String maskedEmail,
            long expiresInSeconds,
            long resendAvailableInSeconds
    ) {
        this.maskedEmail =
                maskedEmail;

        this.expiresInSeconds =
                expiresInSeconds;

        this.resendAvailableInSeconds =
                resendAvailableInSeconds;
    }


    public String getMaskedEmail() {
        return maskedEmail;
    }


    public long getExpiresInSeconds() {
        return expiresInSeconds;
    }


    public long getResendAvailableInSeconds() {
        return resendAvailableInSeconds;
    }
}