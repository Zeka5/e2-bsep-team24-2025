package com.example.bsep_backend.mapper;

import com.example.bsep_backend.domain.User;
import com.example.bsep_backend.dto.UserDto;
import org.springframework.stereotype.Component;

@Component
public class EntityMapper {
    public UserDto mapUserToDto(User user) {
        UserDto userDto = new UserDto();
        userDto.setId(user.getId());
        userDto.setName(user.getName());
        userDto.setSurname(user.getSurname());
        userDto.setEmail(user.getEmail());
        userDto.setOrganization(user.getOrganization());
        userDto.setRole(user.getRole().name());
        userDto.setCreatedAt(user.getCreatedAt());
        return userDto;
    }
}
