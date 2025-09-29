package com.example.bsep_backend.service.intr;

import com.example.bsep_backend.domain.User;
import com.example.bsep_backend.dto.CreateCAUserRequest;
import com.example.bsep_backend.dto.UserDto;

import java.util.List;

public interface UserService {
    List<UserDto> getAll();
    User getLogedInUser();
    UserDto getMyInfo();
    UserDto updateMyInfo(UserDto userDto);
    void delete(Long userId);
    UserDto createCAUser(CreateCAUserRequest request);
}
