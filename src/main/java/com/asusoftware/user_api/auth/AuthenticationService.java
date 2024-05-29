package com.asusoftware.user_api.auth;


import com.asusoftware.user_api.config.JwtService;
import com.asusoftware.user_api.exceptions.FileStorageException;
import com.asusoftware.user_api.exceptions.UserNotFoundException;
import com.asusoftware.user_api.model.Role;
import com.asusoftware.user_api.model.User;
import com.asusoftware.user_api.repository.UserRepository;
import com.asusoftware.user_api.service.EmailService;
import com.asusoftware.user_api.service.UserService;
import com.asusoftware.user_api.token.model.Token;
import com.asusoftware.user_api.token.model.TokenPurpose;
import com.asusoftware.user_api.token.model.TokenType;
import com.asusoftware.user_api.token.repository.TokenRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    @Value("${external-link.server-mail-url}")
    private String serverMailUrl;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final TokenRepository tokenRepository;
    private final UserService userService;
    private final EmailService emailService;

    @Value("${upload.dir}")
    private String uploadDir;


    /**
     * @param request
     * @return
     */
    public AuthenticationResponse register(RegisterRequest request, MultipartFile file) throws Exception {
        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setBirthday(request.getBirthday());
        user.setGender(request.getGender());
        user.setRole(request.getRole());
        user.setEnabled(false); // User is disabled until email confirmation
        var savedUser= userRepository.save(user);

        // Create and save confirmation token
        Token confirmationToken = createConfirmationToken(savedUser);
        tokenRepository.save(confirmationToken);

        // Send confirmation email
        sendConfirmationEmail(savedUser, confirmationToken.getToken());

//        if (file != null && !file.isEmpty()) {
//            userService.uploadProfileImage(
//                    file,
//                    savedUser.getId()
//            );
//        } else {
//            setDefaultProfileImage(savedUser.getId());
//        }


        var jwtToken = jwtService.generateToken(savedUser, savedUser.getId());
        var refreshToken = jwtService.generateRefreshToken(savedUser, savedUser.getId());
        saveUserToken(savedUser, jwtToken);
        return AuthenticationResponse.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken)
                .message("Registration successful. Please check your email to confirm your account.")
                .build();
    }

    public void confirmUser(String token) {
        Token confirmationToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid token"));

        if (confirmationToken.getTokenPurpose() != TokenPurpose.EMAIL_CONFIRMATION) {
            throw new IllegalArgumentException("Token not intended for email confirmation");
        }

        User user = confirmationToken.getUser();
        user.setEnabled(true);
        userRepository.save(user);

        // Optionally, mark the token as expired
        confirmationToken.setExpired(true);
        tokenRepository.save(confirmationToken);
    }


    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        var user = userRepository.findByEmail(request.getEmail()).orElseThrow();
        var jwtToken = jwtService.generateToken(user, user.getId());
        var refreshToken = jwtService.generateRefreshToken(user, user.getId());
        revokeAllUserTokens(user);
        saveUserToken(user, jwtToken);
        return AuthenticationResponse.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken)
                .build();
    }

    private void saveUserToken(User user, String jwtToken) {
        var token = Token.builder()
                .user(user)
                .token(jwtToken)
                .tokenType(TokenType.BEARER)
                .tokenPurpose(TokenPurpose.AUTHENTICATION)
                .createdDate(new Date())
                .expired(false)
                .revoked(false)
                .build();
        tokenRepository.save(token);
    }

    public void setDefaultProfileImage(UUID userId) {
        // Find the user by ID
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));

        String uploadPath = uploadDir + "/" + userId;
        Path uploadDirPath = Paths.get(uploadPath);

        try {
            // Ensure the user-specific directory exists
            if (!Files.exists(uploadDirPath)) {
                Files.createDirectories(uploadDirPath);
            }

            // Define the default profile image path and the filename for the user-specific copy
            Path defaultImagePath = Paths.get(uploadDir, "newUserDefaultProfileImage", "profile-image.png");
            String fileName = "default-profile.png"; // Or generate a unique name if preferred

            // Copy the default image to the user's directory
            Path targetLocation = uploadDirPath.resolve(fileName);
            Files.copy(defaultImagePath, targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // If the user already has a profile image different from the default, consider deleting the old file
            // This part is optional and depends on your application's requirements
            if (user.getProfileImage() != null && !user.getProfileImage().equals(uploadPath + "/" + fileName)) {
                Path oldFilePath = uploadDirPath.resolve(user.getProfileImage());
                if (Files.exists(oldFilePath)) {
                    Files.delete(oldFilePath); // Delete the old image
                }
            }

            // Update the user's profile image path with the default image's path or filename
            user.setProfileImage(fileName); // Adjust according to how you handle image paths
            userRepository.save(user);

        } catch (IOException e) {
            throw new FileStorageException("Could not set default profile image for user with ID: " + userId, e);
        }
    }


    private void revokeAllUserTokens(User user) {
        var validUserTokens = tokenRepository.findAllValidTokenByUser(user.getId());
        if (validUserTokens.isEmpty())
            return;
        validUserTokens.forEach(token -> {
            token.setExpired(true);
            token.setRevoked(true);
        });
        tokenRepository.saveAll(validUserTokens);
    }

    public void refreshToken(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        final String refreshToken;
        final String userEmail;
        if (authHeader == null ||!authHeader.startsWith("Bearer ")) {
            return;
        }
        refreshToken = authHeader.substring(7);
        userEmail = jwtService.extractUsername(refreshToken);
        if (userEmail != null) {
            var user = this.userRepository.findByEmail(userEmail)
                    .orElseThrow();
            if (jwtService.isTokenValid(refreshToken, user)) {
                var accessToken = jwtService.generateToken(user, user.getId());
                revokeAllUserTokens(user);
                saveUserToken(user, accessToken);
                var authResponse = AuthenticationResponse.builder()
                        .accessToken(accessToken)
                        .refreshToken(refreshToken)
                        .build();
                new ObjectMapper().writeValue(response.getOutputStream(), authResponse);
            }
        }
    }

    private String generateActivationCode() {
        // Implementați generarea codului de activare unic
        // Returnați codul generat
        return UUID.randomUUID().toString();
    }

    public void activateUser(String activationCode) {
        // Implementați actualizarea stării utilizatorului pentru a-l activa
        User user = userRepository.findByActivationCode(activationCode).orElseThrow();
        // Actualizați starea utilizatorului pentru a-l activa
        user.setEnabled(true);
        // userRepository.updateUserStatus(activationCode);
        userRepository.save(user);
    }

    private Token createConfirmationToken(User user) {
        return Token.builder()
                .token(UUID.randomUUID().toString())
                .tokenType(TokenType.BEARER)
                .tokenPurpose(TokenPurpose.EMAIL_CONFIRMATION)
                .createdDate(new Date())
                .user(user)
                .revoked(false)
                .expired(false)
                .build();
    }

    private void sendConfirmationEmail(User user, String token) throws MessagingException {
        String confirmationUrl = serverMailUrl + "?token=" + token;
        String message = "Please click the link below to confirm your email address: \n" + confirmationUrl;
        emailService.sendEmail(user.getEmail(), "Email Confirmation", message);
    }
}
