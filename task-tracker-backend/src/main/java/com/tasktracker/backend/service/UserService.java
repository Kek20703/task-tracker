package com.tasktracker.backend.service;

import com.tasktracker.backend.dto.RegisterRequest;
import com.tasktracker.backend.dto.UserCredentials;
import com.tasktracker.backend.dto.UserResponse;
import com.tasktracker.backend.entity.User;
import com.tasktracker.backend.exception.AccountNotFoundException;
import com.tasktracker.backend.exception.EmailAlreadyTakenException;
import com.tasktracker.backend.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyTakenException();
        }
        User user = new User(email, passwordEncoder.encode(request.password()));
        try {
            user = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            // Два одновременных запроса с одним email: проверку выше прошли оба,
            // но уникальный индекс в БД пропустил только один.
            throw new EmailAlreadyTakenException();
        }
        return toResponse(user);
    }

    /**
     * Пользователь по id из токена. Если его уже удалили, токен больше ничего не значит.
     * Все запросы «от имени текущего пользователя» должны начинаться с этой проверки.
     */
    public UserResponse getById(UUID id) {
        return userRepository.findById(id).map(UserService::toResponse).orElseThrow(AccountNotFoundException::new);
    }

    /** Проверка без загрузки пользователя — для операций, которым нужен только сам факт его существования. */
    public void requireExisting(UUID id) {
        if (!userRepository.existsById(id)) {
            throw new AccountNotFoundException();
        }
    }

    /** Поиск без учёта регистра — так же, как email сохраняется при регистрации. */
    public Optional<UserResponse> findByEmail(String email) {
        return userRepository.findByEmail(normalizeEmail(email)).map(UserService::toResponse);
    }

    /** Данные для проверки пароля. Наружу из сервиса уходит запись, а не JPA-сущность. */
    public Optional<UserCredentials> findCredentialsByEmail(String email) {
        return userRepository.findByEmail(normalizeEmail(email))
                .map(user -> new UserCredentials(user.getEmail(), user.getPassword()));
    }

    private static UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getEmail());
    }

    private static String normalizeEmail(String email) {
        return email.toLowerCase(Locale.ROOT);
    }
}
