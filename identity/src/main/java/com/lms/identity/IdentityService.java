package com.lms.identity;

import com.lms.identity.entity.UserEntity;
import com.lms.identity.repo.UserRepository;
import com.lms.shared.IdentityPort;
import com.lms.shared.UserSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IdentityService implements IdentityPort {
    private final UserRepository users;

    @Override
    @Transactional(readOnly = true, transactionManager = "identityTx")
    public UserSnapshot getUser(String userId) {
        return users.findById(userId).map(this::toSnapshot).orElse(null);
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "identityTx")
    public UserSnapshot validateSession(String userId) {
        if (userId == null || userId.isBlank()) {
            return null;
        }
        return getUser(userId);
    }

    private UserSnapshot toSnapshot(UserEntity user) {
        return new UserSnapshot(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getStatus(),
                user.getUserRoles().stream().map(ur -> ur.getRole().getCode()).collect(Collectors.toList())
        );
    }
}
