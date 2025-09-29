package com.example.bsep_backend.service.impl;

import com.example.bsep_backend.domain.User;
import com.example.bsep_backend.domain.UserRole;
import com.example.bsep_backend.dto.CreateCAUserRequest;
import com.example.bsep_backend.dto.UserDto;
import com.example.bsep_backend.exception.EntityExistsException;
import com.example.bsep_backend.exception.NotFoundException;
import com.example.bsep_backend.mapper.EntityMapper;
import com.example.bsep_backend.repository.UserRepository;
import com.example.bsep_backend.service.intr.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final EntityMapper entityMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public List<UserDto> getAll() {
        List<User> users = userRepository.findAll();
        return users.stream()
                .map(entityMapper::mapUserToDto)
                .toList();

    }

    @Override
    public User getLogedInUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(()-> new UsernameNotFoundException("User not found"));
    }

    @Override
    public UserDto getMyInfo() {
        User user = getLogedInUser();
        return entityMapper.mapUserToDto(user);
    }

    @Override
    public UserDto updateMyInfo(UserDto userDto) {
        User user = getLogedInUser();

        if (userDto.getEmail().equalsIgnoreCase(user.getEmail())
                && userRepository.existsByEmail(userDto.getEmail())) {
            throw new EntityExistsException("Email is already in use.");
        }

        if(userDto.getEmail() != null) {
            user.setEmail(userDto.getEmail());
        }

        User saved = userRepository.save(user);
        return entityMapper.mapUserToDto(saved);
    }

    @Override
    public void delete(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(()-> new NotFoundException("User not found"));

        userRepository.delete(user);
    }

    @Override
    public UserDto createCAUser(CreateCAUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EntityExistsException("Email is already in use.");
        }

        String defaultPassword = "1234";

        User caUser = User.builder()
                .name(request.getName())
                .surname(request.getSurname())
                .email(request.getEmail())
                .organization(request.getOrganization())
                .password(passwordEncoder.encode(defaultPassword))
                .role(UserRole.CA)
                .isActivated(true)
                .build();

        User savedUser = userRepository.save(caUser);

        log.info("CA user created with email: {} and default password: {}",
                request.getEmail(), defaultPassword);

        return entityMapper.mapUserToDto(savedUser);
    }

}
