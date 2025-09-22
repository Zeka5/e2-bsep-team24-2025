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
import com.example.bsep_backend.service.intr.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EntityMapper entityMapper;
    private final JwtUtils jwtUtils;
    private final EmailService emailService;

    @Override
    public UserDto register(UserDto userDto) {
        if (userRepository.existsByEmail(userDto.getEmail())) {
            throw new EntityExistsException("Email is already in use.");
        }
        if (userRepository.existsByUsername(userDto.getUsername())) {
            throw new EntityExistsException("Username is already in use.");
        }

        UserRole role = UserRole.USER;

        if(userDto.getAvatarId() == null){
            userDto.setAvatarId(1);
        }

        String activationToken = UUID.randomUUID().toString();
        LocalDateTime tokenExpiry = LocalDateTime.now().plusHours(24);

        User user = User.builder()
                .username(userDto.getUsername())
                .email(userDto.getEmail())
                .password(passwordEncoder.encode(userDto.getPassword()))
                .role(role)
                .avatarId(userDto.getAvatarId())
                .isActivated(false)
                .activationToken(activationToken)
                .tokenExpiry(tokenExpiry)
                .build();
        System.out.println("User creted, SENDING EMAIL");

        emailService.sendActivationEmail(userDto.getEmail(), userDto.getUsername(), activationToken);
        System.out.println("Email sent, SAVING USER");
        User savedUser = userRepository.save(user);

        return entityMapper.mapUserToDto(savedUser);
    }

    @Override
    public AuthData login(LoginRequest loginRequest) {
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
