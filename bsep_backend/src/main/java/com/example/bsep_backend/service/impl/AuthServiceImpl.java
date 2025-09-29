package com.example.bsep_backend.service.impl;

import com.example.bsep_backend.domain.User;
import com.example.bsep_backend.domain.UserRole;
import com.example.bsep_backend.dto.AuthData;
import com.example.bsep_backend.dto.LoginRequest;
import com.example.bsep_backend.dto.UserDto;
import com.example.bsep_backend.exception.EntityExistsException;
import com.example.bsep_backend.exception.InvalidCredentialsException;
import com.example.bsep_backend.exception.NotFoundException;
import com.example.bsep_backend.mapper.EntityMapper;
import com.example.bsep_backend.repository.UserRepository;
import com.example.bsep_backend.security.jwt.JwtUtils;
import com.example.bsep_backend.service.intr.AuthService;
import com.example.bsep_backend.service.intr.CaptchaService;
import com.example.bsep_backend.service.intr.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EntityMapper entityMapper;
    private final JwtUtils jwtUtils;
    private final EmailService emailService;
    private final CaptchaService captchaService;

    @Override
    public UserDto register(UserDto userDto) {
        if (userDto.getName() == null || userDto.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be null or empty.");
        }
        if (userDto.getSurname() == null || userDto.getSurname().trim().isEmpty()) {
            throw new IllegalArgumentException("Surname cannot be null or empty.");
        }
        if (userDto.getEmail() == null || userDto.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty.");
        }
        if (userDto.getPassword() == null || userDto.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty.");
        }
        if (userDto.getOrganization() == null || userDto.getOrganization().trim().isEmpty()) {
            throw new IllegalArgumentException("Organization cannot be null or empty.");
        }

        if (userRepository.existsByEmail(userDto.getEmail())) {
            throw new EntityExistsException("Email is already in use.");
        }

        UserRole role;
        try {
            if (userDto.getRole() != null && !userDto.getRole().trim().isEmpty()) {
                role = UserRole.fromString(userDto.getRole());
            } else {
                role = UserRole.USER;
            }
        } catch (IllegalArgumentException e) {
            role = UserRole.USER;
        }
        String activationToken = UUID.randomUUID().toString();
        LocalDateTime tokenExpiry = LocalDateTime.now().plusHours(24);

        User user = User.builder()
                .name(userDto.getName())
                .surname(userDto.getSurname())
                .email(userDto.getEmail())
                .organization(userDto.getOrganization())
                .password(passwordEncoder.encode(userDto.getPassword()))
                .role(role)
                .isActivated(false)
                .activationToken(activationToken)
                .tokenExpiry(tokenExpiry)
                .build();

        emailService.sendActivationEmail(userDto.getEmail(), userDto.getName(), activationToken);
        User savedUser = userRepository.save(user);

        return entityMapper.mapUserToDto(savedUser);
    }

    @Override
    public AuthData login(LoginRequest loginRequest) {
        // Verify CAPTCHA first
        captchaService.verifyCaptcha(loginRequest.getCaptchaToken());

        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new NotFoundException("User with provided email not found"));

        if(!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Wrong password");
        }

        if(!user.isActivated()) {
            throw new InvalidCredentialsException("Account is not activated. Please check your email and activate your account.");
        }

        String token = jwtUtils.generateTokenWithUserInfo(user);
        return AuthData.builder()
                .user(entityMapper.mapUserToDto(user))
                .token(token)
                .expirationDate(jwtUtils.getExpirationDate())
                .build();
    }

    @Override
    public String activateAccount(String activationToken) {
        User user = userRepository.findByActivationToken(activationToken)
                .orElseThrow(() -> new NotFoundException("Invalid activation token"));

        if (user.getTokenExpiry().isBefore(LocalDateTime.now())) {
            userRepository.delete(user);
            throw new InvalidCredentialsException("Activation token has expired. Please register again.");
        }

        if (user.isActivated()) {
            return "Account is already activated";
        }

        user.setActivated(true);
        user.setActivationToken(null);
        user.setTokenExpiry(null);
        userRepository.save(user);

        return "Account activated successfully! You can now log in.";
    }
}
