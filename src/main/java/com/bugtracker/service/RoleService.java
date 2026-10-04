package com.bugtracker.service;

import com.bugtracker.entity.Role;
import com.bugtracker.entity.RoleName;
import com.bugtracker.repository.RoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Provides access to the reference data in the {@code roles} table,
 * creating the well known roles on demand.
 */
@Service
public class RoleService {

    private final RoleRepository roleRepository;

    public RoleService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Transactional
    public Role getOrCreate(RoleName roleName) {
        return roleRepository.findByName(roleName)
                .orElseGet(() -> roleRepository.save(new Role(roleName, describe(roleName))));
    }

    private static String describe(RoleName roleName) {
        return switch (roleName) {
            case ROLE_ADMIN -> "Full access: manage bugs and users";
            case ROLE_TESTER -> "Report bugs, comment and attach evidence";
            case ROLE_DEVELOPER -> "Work on assigned bugs and update status";
        };
    }
}
