package com.example.bsep_backend.controller;

import com.example.bsep_backend.dto.CreateCAUserRequest;
import com.example.bsep_backend.dto.UserDto;
import com.example.bsep_backend.service.intr.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/get-all")
//    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserDto>> getAll() {
        return ResponseEntity.ok(userService.getAll());
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> getMyInfo() {
        return ResponseEntity.ok(userService.getMyInfo());
    }

    @PutMapping("/me")
    public ResponseEntity<UserDto> updateMyInfo(@RequestBody UserDto userDto) {
        return ResponseEntity.ok(userService.updateMyInfo(userDto));
    }

    @PostMapping("/ca-users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createCAUser(@Valid @RequestBody CreateCAUserRequest request) {
        try {
            UserDto caUser = userService.createCAUser(request);
            return ResponseEntity.ok(caUser);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error creating CA user: " + e.getMessage());
        }
    }

    @DeleteMapping("{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.ok("Successfully deleted user");
    }
}
