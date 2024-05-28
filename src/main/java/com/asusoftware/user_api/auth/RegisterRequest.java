package com.asusoftware.user_api.auth;
import com.asusoftware.user_api.model.Gender;
import com.asusoftware.user_api.model.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {

    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private Date birthday;
    private Gender gender;
    private Role role;
}
