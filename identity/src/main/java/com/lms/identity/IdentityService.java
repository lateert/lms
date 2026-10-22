package com.lms.identity;

import com.lms.identity.entity.RoleEntity;
import com.lms.identity.entity.UserEntity;
import com.lms.identity.entity.UserRoleEntity;
import com.lms.identity.repo.RoleRepository;
import com.lms.identity.repo.UserRepository;
import com.lms.identity.repo.UserRoleRepository;
import com.lms.shared.AppException;
import com.lms.shared.EventPublisher;
import com.lms.shared.IdentityPort;
import com.lms.shared.Roles;
import com.lms.shared.Times;
import com.lms.shared.UserSnapshot;
import com.lms.shared.event.UserRegistered;
import com.lms.shared.input.AssignRoleInput;
import com.lms.shared.input.LoginInput;
import com.lms.shared.input.RegisterInput;
import com.lms.shared.view.AuthPayloadView;
import com.lms.shared.view.UserView;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IdentityService implements IdentityPort {
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final int MIN_PASSWORD = 6;

    private final UserRepository users;
    private final RoleRepository roles;
    private final UserRoleRepository userRoles;
    private final EventPublisher events;
    private final PasswordEncoder passwords = new BCryptPasswordEncoder();
    // Сессии живут в памяти процесса: после перезапуска шлюза нужно войти заново.
    private final Map<String, String> sessions = new ConcurrentHashMap<>();

    @Transactional(transactionManager = "identityTx")
    public AuthPayloadView register(RegisterInput input) {
        String email = normalizeEmail(input.email());
        if (!EMAIL.matcher(email).matches()) {
            throw AppException.badRequest("Некорректный email");
        }
        if (input.password() == null || input.password().length() < MIN_PASSWORD) {
            throw AppException.badRequest("Пароль должен содержать не менее " + MIN_PASSWORD + " символов");
        }
        String fullName = input.fullName() == null ? "" : input.fullName().trim();
        if (fullName.isEmpty()) {
            throw AppException.badRequest("Укажите полное имя");
        }
        if (users.existsByEmail(email)) {
            throw AppException.conflict("Пользователь с таким email уже зарегистрирован");
        }
        RoleEntity student = roles.findByCode(Roles.STUDENT)
                .orElseThrow(() -> AppException.notFound("Роль student не найдена"));
        UserEntity user = users.save(new UserEntity(
                UUID.randomUUID().toString(), email, passwords.encode(input.password()), fullName, "active", Times.now()
        ));
        user.getUserRoles().add(userRoles.save(new UserRoleEntity(UUID.randomUUID().toString(), user, student)));
        events.publish(new UserRegistered(user.getId(), email, fullName));
        return issueToken(user);
    }

    @Transactional(readOnly = true, transactionManager = "identityTx")
    public AuthPayloadView login(LoginInput input) {
        UserEntity user = users.findByEmail(normalizeEmail(input.email()))
                .filter(u -> input.password() != null && passwords.matches(input.password(), u.getPasswordHash()))
                .orElseThrow(() -> AppException.unauthorized("Неверный email или пароль"));
        if (!"active".equals(user.getStatus())) {
            throw AppException.forbidden("Учётная запись заблокирована");
        }
        return issueToken(user);
    }

    @Transactional(transactionManager = "identityTx")
    public UserView assignRole(UserSnapshot actor, AssignRoleInput input) {
        if (!actor.isAdmin()) {
            throw AppException.forbidden("Назначать роли может только администратор");
        }
        UserEntity user = users.findById(input.userId())
                .orElseThrow(() -> AppException.notFound("Пользователь не найден"));
        RoleEntity role = roles.findByCode(input.roleCode())
                .orElseThrow(() -> AppException.notFound("Роль не найдена: " + input.roleCode()));
        boolean alreadyHas = user.getUserRoles().stream().anyMatch(ur -> ur.getRole().getId().equals(role.getId()));
        if (alreadyHas) {
            throw AppException.conflict("У пользователя уже есть роль " + role.getCode());
        }
        user.getUserRoles().add(userRoles.save(new UserRoleEntity(UUID.randomUUID().toString(), user, role)));
        return UserView.from(toSnapshot(user));
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "identityTx")
    public UserSnapshot getUser(String userId) {
        return users.findById(userId).map(this::toSnapshot).orElse(null);
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "identityTx")
    public UserSnapshot validateSession(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        String userId = sessions.get(token);
        if (userId == null) {
            return null;
        }
        UserSnapshot user = getUser(userId);
        return user != null && "active".equals(user.status()) ? user : null;
    }

    private AuthPayloadView issueToken(UserEntity user) {
        String token = UUID.randomUUID().toString();
        sessions.put(token, user.getId());
        return new AuthPayloadView(token, UserView.from(toSnapshot(user)));
    }

    private static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
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
