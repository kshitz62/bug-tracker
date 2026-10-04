package com.bugtracker.service;

import com.bugtracker.dto.UserResponse;
import com.bugtracker.entity.Role;
import com.bugtracker.entity.RoleName;
import com.bugtracker.entity.User;
import com.bugtracker.exception.BadRequestException;
import com.bugtracker.exception.DuplicateResourceException;
import com.bugtracker.exception.ResourceNotFoundException;
import com.bugtracker.mapper.UserMapper;
import com.bugtracker.repository.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * User lifecycle operations: lookups, registration bookkeeping and
 * administrator driven role / status changes.
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleService roleService;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       RoleService roleService,
                       UserMapper userMapper,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleService = roleService;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public User getEntityById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    @Transactional(readOnly = true)
    public User getEntityByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        return userMapper.toResponse(getEntityById(id));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserResponse> getAllUsers() {
        return userMapper.toResponseList(userRepository.findAll());
    }

    /**
     * Users that can be assigned to a bug: developers and administrators.
     */
    @Transactional(readOnly = true)
    public List<UserResponse> getAssignableUsers() {
        Set<User> assignable = new LinkedHashSet<>(userRepository.findAllByRoleName(RoleName.ROLE_DEVELOPER));
        assignable.addAll(userRepository.findAllByRoleName(RoleName.ROLE_ADMIN));
        return userMapper.toResponseList(assignable);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getDevelopers() {
        return userMapper.toResponseList(userRepository.findAllByRoleName(RoleName.ROLE_DEVELOPER));
    }

    /**
     * Creates a new user after validating uniqueness of the email address.
     * Passwords are always stored as BCrypt hashes.
     */
    @Transactional
    public User createUser(String fullName, String rawPassword, String email, RoleName roleName) {
        String normalisedEmail = email == null ? null : email.trim().toLowerCase();
        if (normalisedEmail == null || normalisedEmail.isBlank()) {
            throw new BadRequestException("Email is required.");
        }
        if (userRepository.existsByEmailIgnoreCase(normalisedEmail)) {
            throw new DuplicateResourceException("An account with email '" + normalisedEmail + "' already exists.");
        }
        Role role = roleService.getOrCreate(roleName);
        User user = new User(fullName.trim(), normalisedEmail, passwordEncoder.encode(rawPassword));
        user.addRole(role);
        return userRepository.save(user);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse updateRole(Long userId, RoleName roleName) {
        User user = getEntityById(userId);
        Role role = roleService.getOrCreate(roleName);
        user.getRoles().clear();
        user.addRole(role);
        return userMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse updateEnabled(Long userId, boolean enabled) {
        User user = getEntityById(userId);
        user.setEnabled(enabled);
        return userMapper.toResponse(userRepository.save(user));
    }
}
