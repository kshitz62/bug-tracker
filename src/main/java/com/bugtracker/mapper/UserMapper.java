package com.bugtracker.mapper;

import com.bugtracker.dto.UserResponse;
import com.bugtracker.entity.User;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

/**
 * Converts {@link User} entities into API representations.
 */
@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        if (user == null) {
            return null;
        }
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPrimaryRole(),
                user.isEnabled(),
                user.getCreatedAt()
        );
    }

    public List<UserResponse> toResponseList(Collection<User> users) {
        return users.stream().map(this::toResponse).toList();
    }
}
