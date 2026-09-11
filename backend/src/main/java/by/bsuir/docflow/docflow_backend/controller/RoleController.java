package by.bsuir.docflow.docflow_backend.controller;

import by.bsuir.docflow.docflow_backend.entity.Role;
import by.bsuir.docflow.docflow_backend.repository.RoleRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
public class RoleController {

    private final RoleRepository roleRepository;

    public RoleController(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @GetMapping
    public List<Role> getRoles() {
        return roleRepository.findAll();
    }
}