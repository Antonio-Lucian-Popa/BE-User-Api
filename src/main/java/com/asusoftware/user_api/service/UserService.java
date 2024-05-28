package com.asusoftware.user_api.service;

import com.asusoftware.user_api.model.User;
import com.asusoftware.user_api.model.dto.UserDto;
import com.asusoftware.user_api.model.dto.UserResponseDto;
import com.asusoftware.user_api.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@AllArgsConstructor
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserResponseDto saveUser(UserDto userDto) {
        User user = UserDto.toEntity(userDto);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return UserResponseDto.toDto(userRepository.save(user));
    }

    public UserResponseDto getUserById(UUID id) {
        User user = userRepository.findById(id).orElseThrow();
        return UserResponseDto.toDto(user);
    }

    public UserResponseDto findByEmail(String email) {
        return UserResponseDto.toDto(userRepository.findByEmail(email));
    }
}
