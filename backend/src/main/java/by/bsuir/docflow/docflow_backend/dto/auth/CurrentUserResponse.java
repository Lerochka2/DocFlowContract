package by.bsuir.docflow.docflow_backend.dto.auth;

public class CurrentUserResponse {

    private final Long userId;
    private final String login;
    private final String fullName;
    private final String role;

    public CurrentUserResponse(
            Long userId,
            String login,
            String fullName,
            String role
    ) {
        this.userId = userId;
        this.login = login;
        this.fullName = fullName;
        this.role = role;
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

    public String getRole() {
        return role;
    }
}