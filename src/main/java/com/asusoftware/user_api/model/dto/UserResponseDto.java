package com.asusoftware.user_api.model.dto;

import com.asusoftware.user_api.model.Gender;
import com.asusoftware.user_api.model.Role;
import com.asusoftware.user_api.model.User;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class UserResponseDto {
    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
    private LocalDate birthday;
    private Gender gender;
    private String profileImage;
    private Role role;
    private boolean enabled;

    public static UserResponseDto toDto(User user) {
        UserResponseDto userResponseDto = new UserResponseDto();
        userResponseDto.setId(user.getId());
        userResponseDto.setFirstName(user.getFirstName());
        userResponseDto.setLastName(user.getLastName());
        userResponseDto.setBirthday(user.getBirthday());
        userResponseDto.setEmail(user.getEmail());
        userResponseDto.setRole(user.getRole());
        userResponseDto.setGender(user.getGender());
        userResponseDto.setEnabled(user.isEnabled());
        return userResponseDto;
    }

    public static User toEntity(UserResponseDto userResponseDto) {
        User user = new User();
        user.setId(userResponseDto.getId());
        user.setFirstName(userResponseDto.getFirstName());
        user.setLastName(userResponseDto.getLastName());
        user.setEmail(userResponseDto.getEmail());
        user.setBirthday(userResponseDto.getBirthday());
        user.setRole(userResponseDto.getRole());
        user.setGender(userResponseDto.getGender());
        user.setEnabled(userResponseDto.isEnabled());
        return user;
    }
}
