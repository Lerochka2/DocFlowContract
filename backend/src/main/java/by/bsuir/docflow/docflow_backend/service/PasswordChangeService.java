package by.bsuir.docflow.docflow_backend.service;

import by.bsuir.docflow.docflow_backend.dto.auth.ConfirmPasswordChangeRequest;
import by.bsuir.docflow.docflow_backend.dto.auth.PasswordCodeRequestResponse;
import by.bsuir.docflow.docflow_backend.dto.auth.PasswordCodeVerificationResponse;
import by.bsuir.docflow.docflow_backend.entity.AppUser;
import by.bsuir.docflow.docflow_backend.entity.PasswordVerificationCode;
import by.bsuir.docflow.docflow_backend.repository.AppUserRepository;
import by.bsuir.docflow.docflow_backend.repository.PasswordVerificationCodeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

@Service
public class PasswordChangeService {

    private static final int CODE_TTL_SECONDS =
            300;

    private static final int RESEND_COOLDOWN_SECONDS =
            60;

    private static final int TOKEN_TTL_SECONDS =
            600;

    private static final int MAX_ATTEMPTS =
            5;


    private final AppUserRepository userRepository;

    private final PasswordVerificationCodeRepository
            verificationRepository;

    private final PasswordVerificationMailService
            mailService;

    private final PasswordEncoder passwordEncoder;

    private final SecureRandom secureRandom =
            new SecureRandom();


    public PasswordChangeService(
            AppUserRepository userRepository,
            PasswordVerificationCodeRepository verificationRepository,
            PasswordVerificationMailService mailService,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository =
                userRepository;

        this.verificationRepository =
                verificationRepository;

        this.mailService =
                mailService;

        this.passwordEncoder =
                passwordEncoder;
    }


    @Transactional
    public PasswordCodeRequestResponse requestCode(
            Long userId
    ) {

        AppUser user =
                getUser(
                        userId
                );

        String email =
                user.getEmail();

        if (
                email == null
                ||
                email.isBlank()
        ) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Для учетной записи не указан email"
            );
        }

        LocalDateTime now =
                LocalDateTime.now();


        verificationRepository
                .findTopByUser_IdOrderByCreatedAtDesc(
                        userId
                )
                .ifPresent(
                        latest -> {

                            LocalDateTime resendAt =
                                    latest
                                            .getCreatedAt()
                                            .plusSeconds(
                                                    RESEND_COOLDOWN_SECONDS
                                            );

                            if (
                                    resendAt.isAfter(
                                            now
                                    )
                            ) {

                                long seconds =
                                        Duration
                                                .between(
                                                        now,
                                                        resendAt
                                                )
                                                .getSeconds()
                                                + 1;

                                throw new ResponseStatusException(
                                        HttpStatus.TOO_MANY_REQUESTS,
                                        "Повторная отправка будет доступна через "
                                                + seconds
                                                + " сек."
                                );
                            }
                        }
                );


        List<PasswordVerificationCode> activeCodes =
                verificationRepository
                        .findByUser_IdAndUsedFalse(
                                userId
                        );

        for (
                PasswordVerificationCode activeCode
                :
                activeCodes
        ) {
            activeCode.setUsed(
                    true
            );
        }

        if (!activeCodes.isEmpty()) {

            verificationRepository
                    .saveAll(
                            activeCodes
                    );
        }


        String code =
                String.format(
                        "%06d",
                        secureRandom.nextInt(
                                1_000_000
                        )
                );


        PasswordVerificationCode verification =
                new PasswordVerificationCode();

        verification.setUser(
                user
        );

        verification.setCodeHash(
                passwordEncoder.encode(
                        code
                )
        );

        verification.setExpiresAt(
                now.plusSeconds(
                        CODE_TTL_SECONDS
                )
        );

        verification.setAttemptCount(
                0
        );

        verification.setUsed(
                false
        );

        verificationRepository.save(
                verification
        );


        mailService.sendVerificationCode(
                email,
                user.getFullName(),
                code
        );


        return new PasswordCodeRequestResponse(
                maskEmail(
                        email
                ),
                CODE_TTL_SECONDS,
                RESEND_COOLDOWN_SECONDS
        );
    }


    @Transactional
    public PasswordCodeVerificationResponse verifyCode(
            Long userId,
            String code
    ) {

        PasswordVerificationCode verification =
                getActiveVerification(
                        userId
                );

        LocalDateTime now =
                LocalDateTime.now();


        if (
                verification
                        .getExpiresAt()
                        .isBefore(
                                now
                        )
        ) {

            verification.setUsed(
                    true
            );

            verificationRepository.save(
                    verification
            );

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Срок действия кода истек"
            );
        }


        int attempts =
                verification
                        .getAttemptCount()
                        + 1;

        verification.setAttemptCount(
                attempts
        );


        if (
                attempts > MAX_ATTEMPTS
        ) {

            verification.setUsed(
                    true
            );

            verificationRepository.save(
                    verification
            );

            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Превышено количество попыток. Запросите новый код"
            );
        }


        if (
                !passwordEncoder.matches(
                        code,
                        verification.getCodeHash()
                )
        ) {

            if (
                    attempts >= MAX_ATTEMPTS
            ) {
                verification.setUsed(
                        true
                );
            }

            verificationRepository.save(
                    verification
            );

            int remaining =
                    MAX_ATTEMPTS
                            - attempts;

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Неверный код. Осталось попыток: "
                            + Math.max(
                                    remaining,
                                    0
                            )
            );
        }


        String changeToken =
                generateToken();

        verification.setVerifiedAt(
                now
        );

        verification.setChangeTokenHash(
                passwordEncoder.encode(
                        changeToken
                )
        );

        verification.setChangeTokenExpiresAt(
                now.plusSeconds(
                        TOKEN_TTL_SECONDS
                )
        );

        verificationRepository.save(
                verification
        );


        return new PasswordCodeVerificationResponse(
                changeToken,
                TOKEN_TTL_SECONDS
        );
    }


    @Transactional
    public void confirmPasswordChange(
            Long userId,
            ConfirmPasswordChangeRequest request
    ) {

        if (
                !request
                        .getNewPassword()
                        .equals(
                                request.getConfirmPassword()
                        )
        ) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Новые пароли не совпадают"
            );
        }


        PasswordVerificationCode verification =
                getActiveVerification(
                        userId
                );


        if (
                verification.getVerifiedAt()
                == null
        ) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Сначала подтвердите код из письма"
            );
        }


        LocalDateTime tokenExpiresAt =
                verification
                        .getChangeTokenExpiresAt();

        if (
                tokenExpiresAt == null
                ||
                tokenExpiresAt.isBefore(
                        LocalDateTime.now()
                )
        ) {

            verification.setUsed(
                    true
            );

            verificationRepository.save(
                    verification
            );

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Срок подтверждения смены пароля истек"
            );
        }


        String tokenHash =
                verification
                        .getChangeTokenHash();

        if (
                tokenHash == null
                ||
                !passwordEncoder.matches(
                        request.getChangeToken(),
                        tokenHash
                )
        ) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Недействительный токен подтверждения"
            );
        }


        AppUser user =
                getUser(
                        userId
                );


        if (
                passwordEncoder.matches(
                        request.getNewPassword(),
                        user.getPasswordHash()
                )
        ) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Новый пароль должен отличаться от текущего"
            );
        }


        user.setPasswordHash(
                passwordEncoder.encode(
                        request.getNewPassword()
                )
        );

        userRepository.save(
                user
        );


        verification.setUsed(
                true
        );

        verificationRepository.save(
                verification
        );
    }


    private PasswordVerificationCode getActiveVerification(
            Long userId
    ) {

        return verificationRepository
                .findTopByUser_IdAndUsedFalseOrderByCreatedAtDesc(
                        userId
                )
                .orElseThrow(
                        () ->
                                new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Активный код подтверждения не найден"
                                )
                );
    }


    private AppUser getUser(
            Long userId
    ) {

        AppUser user =
                userRepository
                        .findById(
                                userId
                        )
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.UNAUTHORIZED,
                                                "Пользователь не найден"
                                        )
                        );

        if (
                !Boolean.TRUE.equals(
                        user.getEnabled()
                )
        ) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Учетная запись отключена"
            );
        }

        return user;
    }


    private String generateToken() {

        byte[] bytes =
                new byte[32];

        secureRandom.nextBytes(
                bytes
        );

        return Base64
                .getUrlEncoder()
                .withoutPadding()
                .encodeToString(
                        bytes
                );
    }


    private String maskEmail(
            String email
    ) {

        int atIndex =
                email.indexOf(
                        '@'
                );

        if (
                atIndex <= 1
        ) {
            return "***"
                    + email.substring(
                            Math.max(
                                    atIndex,
                                    0
                            )
                    );
        }

        String local =
                email.substring(
                        0,
                        atIndex
                );

        String domain =
                email.substring(
                        atIndex
                );

        if (
                local.length() == 2
        ) {
            return local.charAt(0)
                    + "*"
                    + domain;
        }

        return local.charAt(0)
                + "*".repeat(
                        Math.max(
                                local.length() - 2,
                                1
                        )
                )
                + local.charAt(
                        local.length() - 1
                )
                + domain;
    }
}