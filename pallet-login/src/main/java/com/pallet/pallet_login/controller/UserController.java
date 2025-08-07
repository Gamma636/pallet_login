package com.pallet.pallet_login.controller;

import com.pallet.pallet_login.dto.LoginDto;
import com.pallet.pallet_login.dto.RegisterDto;
import com.pallet.pallet_login.dto.UserDto;
import com.pallet.pallet_login.entity.UserEntity;
import com.pallet.pallet_login.request.LoginRequest;
import com.pallet.pallet_login.service.AesEncryptionService;
import com.pallet.pallet_login.service.FaceService;
import com.pallet.pallet_login.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final FaceService faceService;
    private final UserService userService;
    private final AesEncryptionService aesEncryptionService;;

    public UserController(FaceService faceService, UserService userService, AesEncryptionService aesEncryptionService) {
        this.faceService = faceService;
        this.userService = userService;
        this.aesEncryptionService = aesEncryptionService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody LoginRequest request) {
        long startTime = System.currentTimeMillis();

        try {
            System.out.println("Registration attempt for username: " + request.getUsername());

            // Validate input
            if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Username is required");
            }

            if (request.getBase64Image() == null || request.getBase64Image().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Face image is required");
            }

            String username = request.getUsername().trim();

            // Check if user already exists
            UserEntity existingUser = userService.findByUsername(username);
            if (existingUser != null) {
                System.out.println("Registration failed: Username " + username + " already exists");
                return ResponseEntity.status(409).body("Username already exists");
            }

            // Create user entity
            UserEntity user = new UserEntity();
            user.setUserName(username);
            user.setMobileNumber(request.getMobileNumber());
            user.setPassword(aesEncryptionService.encrypt(request.getPassword()));

            UserEntity savedUser = userService.save(user);
            System.out.println("User " + username + " saved to database with ID: " + savedUser.getId());

            // Register face
            boolean faceRegistrationSuccess = faceService.registerFace(request.getBase64Image(), username);
            if (!faceRegistrationSuccess) {
                // Rollback user creation if face registration fails
                try {
                    userService.deleteById(savedUser.getId());
                    System.out.println("Face registration failed for user: " + username + ", user record deleted");
                } catch (Exception rollbackException) {
                    System.out.println("Error during rollback: " + rollbackException.getMessage());
                }
                return ResponseEntity.status(500).body("Face registration failed");
            }

            System.out.println("Registration completed successfully for user: " + username + " in " +
                    (System.currentTimeMillis() - startTime) + "ms");

            RegisterDto response = new RegisterDto(
                    savedUser.getId(),
                    savedUser.getUserName(),
                    savedUser.getMobileNumber()
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.out.println("Registration error for username: " + request.getUsername() + ", error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("Registration failed: " + e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestParam String base64Image) {
        try {
            String matchedUsername = faceService.recognizeFaceFromImage(base64Image);

            System.out.println("Matched username from Python: [" + matchedUsername + "]");

            if (matchedUsername == null || matchedUsername.equalsIgnoreCase("Unknown")) {
                return ResponseEntity.status(401).body("Face not recognized.");
            }

            // Add debugging - log the exact username we're searching for
            String trimmedUsername = matchedUsername.trim();
            System.out.println("Searching for user with username: [" + trimmedUsername + "]");
            System.out.println("Username length: " + trimmedUsername.length());
            System.out.println("Username bytes: " + Arrays.toString(trimmedUsername.getBytes()));

            UserEntity user = userService.findByUsername(trimmedUsername);

            if (user == null) {
                System.out.println("No user found for: [" + trimmedUsername + "]");

                // Debug: check what usernames exist in DB
                try {
                    List<String> allUsernames = userService.getAllUsernames();
                    System.out.println("Available users in DB: " + allUsernames);

                    // Check for partial matches
                    for (String dbUsername : allUsernames) {
                        System.out.println("DB Username: [" + dbUsername + "] - Length: " + dbUsername.length());
                        System.out.println("DB Username bytes: " + Arrays.toString(dbUsername.getBytes()));
                        if (dbUsername.equalsIgnoreCase(trimmedUsername)) {
                            System.out.println("Found case-insensitive match!");
                        }
                        if (dbUsername.trim().equals(trimmedUsername)) {
                            System.out.println("Found match after trimming DB username!");
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Error getting all usernames: " + e.getMessage());
                }

                return ResponseEntity.status(404).body("User not found.");
            }

            System.out.println("User found successfully: " + user.getUserName());
            UserDto userDto = new UserDto(user);
            LoginDto response = new LoginDto("success", "Login successful", userDto);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
}