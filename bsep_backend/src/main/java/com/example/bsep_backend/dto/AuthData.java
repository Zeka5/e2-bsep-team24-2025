package com.example.bsep_backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthData {
    private UserDto user;
    private String token;
    private Date expirationDate;
}