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
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {

        if (userRepository.existsByLogin("admin")) {
            return;
        }

        String adminPassword = System.getenv("DOCFLOW_ADMIN_PASSWORD");

        if (adminPassword == null || adminPassword.isBlank()) {
            System.out.println(
                    "DOCFLOW_ADMIN_PASSWORD is not set. Admin user was not created."
            );
            return;
        }

        Role adminRole = roleRepository.findByCode("ADMIN")
                .orElseThrow(() ->
                        new IllegalStateException("ADMIN role not found")
                );

        AppUser admin = new AppUser();

        admin.setLogin("admin");
        admin.setFullName("Администратор системы");
        admin.setPasswordHash(
                passwordEncoder.encode(adminPassword)
        );
        admin.setRole(adminRole);
        admin.setEnabled(true);

        userRepository.save(admin);

        System.out.println("Default administrator account created.");
    }
}