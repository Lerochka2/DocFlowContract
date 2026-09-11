package by.bsuir.docflow.docflow_backend.service;

import by.bsuir.docflow.docflow_backend.dto.auth.LoginRequest;
import by.bsuir.docflow.docflow_backend.dto.auth.LoginResponse;
import by.bsuir.docflow.docflow_backend.entity.AppUser;
import by.bsuir.docflow.docflow_backend.repository.AppUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import by.bsuir.docflow.docflow_backend.exception.InvalidCredentialsException;

@Service
public class AuthService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            AppUserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {

        AppUser user = userRepository.findByLogin(request.getLogin())
                .orElseThrow(() ->
                        new IllegalArgumentException("Неверный логин или пароль")
                );

        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new IllegalStateException("Пользователь заблокирован");
        }

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPasswordHash()
        )) {
            throw new InvalidCredentialsException("Неверный логин или пароль");
        }

        return new LoginResponse(
                user.getId(),
                user.getLogin(),
                user.getFullName(),
                user.getRole().getCode()
        );
    }
}