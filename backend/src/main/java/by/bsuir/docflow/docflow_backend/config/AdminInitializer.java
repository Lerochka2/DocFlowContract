package by.bsuir.docflow.docflow_backend.config;

import by.bsuir.docflow.docflow_backend.entity.AppUser;
import by.bsuir.docflow.docflow_backend.entity.Role;
import by.bsuir.docflow.docflow_backend.repository.AppUserRepository;
import by.bsuir.docflow.docflow_backend.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminInitializer implements CommandLineRunner {

    private final AppUserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminInitializer(
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

    @Override
    public void run(String... args) {

        createUserIfMissing(
                "admin",
                "Администратор системы",
                "ADMIN",
                "DOCFLOW_ADMIN_PASSWORD"
        );

        createUserIfMissing(
                "operator",
                "Делопроизводитель",
                "DOCUMENT_OPERATOR",
                "DOCFLOW_OPERATOR_PASSWORD"
        );

        createUserIfMissing(
                "lawyer",
                "Юрист",
                "LAWYER",
                "DOCFLOW_LAWYER_PASSWORD"
        );

        createUserIfMissing(
                "manager",
                "Менеджер по договорам",
                "CONTRACT_MANAGER",
                "DOCFLOW_MANAGER_PASSWORD"
        );
    }

    private void createUserIfMissing(
            String login,
            String fullName,
            String roleCode,
            String passwordEnvironmentVariable
    ) {

        if (
                userRepository.existsByLogin(
                        login
                )
        ) {
            return;
        }

        String rawPassword =
                System.getenv(
                        passwordEnvironmentVariable
                );

        if (
                rawPassword == null
                        || rawPassword.isBlank()
        ) {

            System.out.println(
                    passwordEnvironmentVariable
                            + " is not set. User '"
                            + login
                            + "' was not created."
            );

            return;
        }

        Role role =
                roleRepository
                        .findByCode(
                                roleCode
                        )
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                roleCode
                                                        + " role not found"
                                        )
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
                        rawPassword
                )
        );

        user.setRole(
                role
        );

        user.setEnabled(
                true
        );

        userRepository.save(
                user
        );

        System.out.println(
                "System user '"
                        + login
                        + "' with role "
                        + roleCode
                        + " created."
        );
    }
}