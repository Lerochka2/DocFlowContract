package by.bsuir.docflow.docflow_backend.dto.auth;

public class LoginResponse {

    private final Long userId;

    private final String login;

    private final String fullName;

    private final String email;

    private final String role;

    private final String accessToken;

    private final String tokenType;


    public LoginResponse(
            Long userId,
            String login,
            String fullName,
            String email,
            String role,
            String accessToken
    ) {

        this.userId =
                userId;

        this.login =
                login;

        this.fullName =
                fullName;

        this.email =
                email;

        this.role =
                role;

        this.accessToken =
                accessToken;

        this.tokenType =
                "Bearer";
    }


    public Long getUserId() {
        return userId;
    }


    public String getLogin() {
        return login;
    }


    public String getFullName() {
        return fullName;
    }


    public String getEmail() {
        return email;
    }


    public String getRole() {
        return role;
    }


    public String getAccessToken() {
        return accessToken;
    }


    public String getTokenType() {
        return tokenType;
    }
}