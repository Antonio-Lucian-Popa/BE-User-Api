package com.asusoftware.user_api.controller;

import com.asusoftware.user_api.model.dto.UserDto;
import com.asusoftware.user_api.model.dto.UserResponseDto;
import com.asusoftware.user_api.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@AllArgsConstructor
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @PostMapping
    public UserResponseDto createUser(@RequestBody UserDto userDTO) {
        return userService.saveUser(userDTO);
    }

    @GetMapping("/{id}")
    public UserResponseDto getUserById(@PathVariable UUID id) {
        return userService.getUserById(id);
    }

    @GetMapping("/email/{email}")
    public UserResponseDto getUserByEmail(@PathVariable String email) {
        return userService.findByEmail(email);
    }

}
