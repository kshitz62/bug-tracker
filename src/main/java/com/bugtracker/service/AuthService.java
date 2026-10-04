package com.bugtracker.service;

import com.bugtracker.dto.AuthResponse;
import com.bugtracker.dto.LoginRequest;
import com.bugtracker.dto.RegisterRequest;
import com.bugtracker.dto.UserResponse;
import com.bugtracker.entity.RoleName;
import com.bugtracker.entity.User;
import com.bugtracker.exception.BadRequestException;
import com.bugtracker.mapper.UserMapper;
import com.bugtracker.security.JwtService;
import com.bugtracker.security.SecurityUtils;
import com.bugtracker.security.UserPrincipal;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registration, login and "who am I" operations.
 */
@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtService jwtService;
    private final UserMapper userMapper;

    public AuthService(AuthenticationManager authenticationManager,
                       UserService userService,
                       JwtService jwtService,
                       UserMapper userMapper) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.jwtService = jwtService;
        this.userMapper = userMapper;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (request.role() == RoleName.ROLE_ADMIN) {
            throw new BadRequestException(
                    "Self registration as ADMIN is not permitted. Please contact an administrator.");
        }
        User user = userService.createUser(
                request.fullName(),
                request.password(),
                request.email(),
                request.role()
        );
        UserPrincipal principal = UserPrincipal.from(user);
        return new AuthResponse(
                jwtService.generateToken(principal),
                jwtService.getExpirationMs(),
                userMapper.toResponse(user)
        );
    }

    /**
     * Verifies credentials through Spring Security. A failure results in
     * {@link org.springframework.security.core.AuthenticationException},
     * translated to HTTP 401 by the global exception handler.
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = request.email() == null ? "" : request.email().trim().toLowerCase();
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.password())
        );
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return new AuthResponse(
                jwtService.generateToken(principal),
                jwtService.getExpirationMs(),
                userMapper.toResponse(userService.getEntityById(principal.getId()))
        );
    }

    @Transactional(readOnly = true)
    public UserResponse currentUser() {
        UserPrincipal principal = SecurityUtils.requireCurrentUser();
        return userMapper.toResponse(userService.getEntityById(principal.getId()));
    }
}
