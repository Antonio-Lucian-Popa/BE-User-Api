package com.asusoftware.user_api.model.dto;

import com.asusoftware.user_api.model.Gender;
import com.asusoftware.user_api.model.Role;
import com.asusoftware.user_api.model.User;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UserDto {

    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private LocalDate birthday;
    private Gender gender;
    private String profileImage;
    private Role role;


    public static UserDto toDto(User user) {
        UserDto userDto = new UserDto();
        userDto.setFirstName(user.getFirstName());
        userDto.setLastName(user.getLastName());
        userDto.setBirthday(user.getBirthday());
        userDto.setEmail(user.getEmail());
        userDto.setGender(user.getGender());
        //TODO: add profileImage link in the future implementation
        return userDto;
    }

    public static User toEntity(UserDto userDto) {
        User user = new User();
        user.setFirstName(userDto.getFirstName());
        user.setLastName(userDto.getLastName());
        user.setBirthday(userDto.getBirthday());
        user.setEmail(userDto.getEmail());
        user.setRole(userDto.getRole());
        user.setGender(user.getGender());
        return user;
    }
}
