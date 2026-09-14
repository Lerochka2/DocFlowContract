package by.bsuir.docflow.docflow_backend.service;

import by.bsuir.docflow.docflow_backend.dto.user.CreateUserRequest;
import by.bsuir.docflow.docflow_backend.dto.user.UpdateUserEnabledRequest;
import by.bsuir.docflow.docflow_backend.dto.user.UpdateUserFullNameRequest;
import by.bsuir.docflow.docflow_backend.dto.user.UpdateUserRoleRequest;
import by.bsuir.docflow.docflow_backend.dto.user.UserResponse;
import by.bsuir.docflow.docflow_backend.entity.AppUser;
import by.bsuir.docflow.docflow_backend.entity.Role;
import by.bsuir.docflow.docflow_backend.repository.AppUserRepository;
import by.bsuir.docflow.docflow_backend.repository.RoleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class UserAdminService {

    private static final Set<String> ALLOWED_ROLES =
            Set.of(
                    "ADMIN",
                    "DOCUMENT_OPERATOR",
                    "LAWYER",
                    "CONTRACT_MANAGER"
            );

    private final AppUserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserAdminService(
            AppUserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository =
                userRepository;

        this.roleRepository =
                roleRepository;

        this.passwordEncoder =
                passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getUsers() {

        return userRepository
                .findAll()
                .stream()
                .sorted(
                        Comparator.comparing(
                                AppUser::getId
                        )
                )
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public UserResponse createUser(
            CreateUserRequest request
    ) {

        String login =
                normalizeLogin(
                        request.login()
                );

        String fullName =
                normalizeRequiredText(
                        request.fullName(),
                        "Необходимо указать ФИО"
                );

        if (
                userRepository.existsByLogin(
                        login
                )
        ) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Пользователь с логином "
                            + login
                            + " уже существует"
            );
        }

        Role role =
                getAllowedRole(
                        request.roleCode()
                );

        AppUser user =
                new AppUser();

        user.setLogin(
                login
        );

        user.setFullName(
                fullName
        );

        user.setPasswordHash(
                passwordEncoder.encode(
                        request.password()
                )
        );

        user.setRole(
                role
        );

        user.setEnabled(
                true
        );

        AppUser saved =
                userRepository.save(
                        user
                );

        return toResponse(
                saved
        );
    }

    @Transactional
    public UserResponse updateFullName(
            Long userId,
            UpdateUserFullNameRequest request
    ) {

        AppUser user =
                getUser(
                        userId
                );

        String fullName =
                normalizeRequiredText(
                        request.fullName(),
                        "Необходимо указать ФИО"
                );

        user.setFullName(
                fullName
        );

        return toResponse(
                userRepository.save(
                        user
                )
        );
    }

    @Transactional
    public UserResponse updateRole(
            Long userId,
            Long currentAdminId,
            UpdateUserRoleRequest request
    ) {

        AppUser user =
                getUser(
                        userId
                );

        Role newRole =
                getAllowedRole(
                        request.roleCode()
                );

        if (
                userId.equals(
                        currentAdminId
                )
                &&
                !"ADMIN".equals(
                        newRole.getCode()
                )
        ) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Нельзя изменить собственную роль администратора"
            );
        }

        user.setRole(
                newRole
        );

        return toResponse(
                userRepository.save(
                        user
                )
        );
    }

    @Transactional
    public UserResponse updateEnabled(
            Long userId,
            Long currentAdminId,
            UpdateUserEnabledRequest request
    ) {

        AppUser user =
                getUser(
                        userId
                );

        if (
                userId.equals(
                        currentAdminId
                )
                &&
                Boolean.FALSE.equals(
                        request.enabled()
                )
        ) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Нельзя заблокировать собственную учетную запись"
            );
        }

        user.setEnabled(
                request.enabled()
        );

        return toResponse(
                userRepository.save(
                        user
                )
        );
    }

    private AppUser getUser(
            Long userId
    ) {

        return userRepository
                .findById(
                        userId
                )
                .orElseThrow(
                        () ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Пользователь с ID "
                                                + userId
                                                + " не найден"
                                )
                );
    }

    private Role getAllowedRole(
            String roleCode
    ) {

        if (
                roleCode == null
                        ||
                        roleCode.isBlank()
        ) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Необходимо указать роль"
            );
        }

        String normalized =
                roleCode
                        .trim()
                        .toUpperCase(
                                Locale.ROOT
                        );

        if (
                !ALLOWED_ROLES.contains(
                        normalized
                )
        ) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Недопустимая роль: "
                            + roleCode
            );
        }

        return roleRepository
                .findByCode(
                        normalized
                )
                .orElseThrow(
                        () ->
                                new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Роль "
                                                + normalized
                                                + " отсутствует в системе"
                                )
                );
    }

    private String normalizeLogin(
            String login
    ) {

        if (
                login == null
                        ||
                        login.isBlank()
        ) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Необходимо указать логин"
            );
        }

        String normalized =
                login
                        .trim()
                        .toLowerCase(
                                Locale.ROOT
                        );

        if (
                !normalized.matches(
                        "[a-z0-9._-]{3,100}"
                )
        ) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Логин может содержать только латинские буквы, цифры, точку, дефис и нижнее подчеркивание"
            );
        }

        return normalized;
    }

    private String normalizeRequiredText(
            String value,
            String errorMessage
    ) {

        if (
                value == null
                        ||
                        value.isBlank()
        ) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    errorMessage
            );
        }

        return value.trim();
    }

    private UserResponse toResponse(
            AppUser user
    ) {

        Role role =
                user.getRole();

        return new UserResponse(
                user.getId(),
                user.getLogin(),
                user.getFullName(),
                role == null
                        ? null
                        : role.getCode(),
                role == null
                        ? null
                        : role.getName(),
                Boolean.TRUE.equals(
                        user.getEnabled()
                ),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.getLastLoginAt()
        );
    }
}