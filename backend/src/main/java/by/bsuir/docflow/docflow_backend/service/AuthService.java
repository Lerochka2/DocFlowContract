package by.bsuir.docflow.docflow_backend.service;

import by.bsuir.docflow.docflow_backend.dto.auth.LoginRequest;
import by.bsuir.docflow.docflow_backend.dto.auth.LoginResponse;
import by.bsuir.docflow.docflow_backend.entity.AppUser;
import by.bsuir.docflow.docflow_backend.exception.InvalidCredentialsException;
import by.bsuir.docflow.docflow_backend.repository.AppUserRepository;
import by.bsuir.docflow.docflow_backend.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            AppUserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {

        AppUser user = userRepository.findByLogin(request.getLogin())
                .orElseThrow(() ->
                        new InvalidCredentialsException(
                                "Неверный логин или пароль"
                        )
                );

        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new InvalidCredentialsException(
                    "Неверный логин или пароль"
            );
        }

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPasswordHash()
        )) {
            throw new InvalidCredentialsException(
                    "Неверный логин или пароль"
            );
        }

        String token = jwtService.generateToken(user);

        return new LoginResponse(
                user.getId(),
                user.getLogin(),
                user.getFullName(),
                user.getRole().getCode(),
                token
        );
    }
}