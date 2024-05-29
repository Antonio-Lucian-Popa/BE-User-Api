package com.asusoftware.user_api.token.model;

import com.asusoftware.user_api.model.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Token {

    @Id
    @GeneratedValue
    public UUID id;

    @Column(unique = true)
    public String token;

    @Enumerated(EnumType.STRING)
    public TokenType tokenType = TokenType.BEARER;

    public boolean revoked;

    public boolean expired;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    public User user;

    // Additional field for distinguishing token purposes (optional)
    @Enumerated(EnumType.STRING)
    @Column(name = "token_purpose")
    private TokenPurpose tokenPurpose;

    @Column(name = "created_date", nullable = false)
    private Date createdDate;
}
