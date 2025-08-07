package com.pallet.pallet_login.service;

import com.pallet.pallet_login.entity.UserEntity;
import com.pallet.pallet_login.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserEntity save(UserEntity user) {
        System.out.println("Saving user: " + user.getUserName());
        return userRepository.save(user);
    }

    public UserEntity findByUsername(String username) {
        System.out.println("Searching for user: [" + username + "]");

        // First try exact match
        UserEntity user = userRepository.findByUserName(username);

        if (user == null) {
            // Try case-insensitive match
            System.out.println("Exact match failed, trying case-insensitive search...");
            user = userRepository.findByUserNameIgnoreCase(username);
        }

        if (user == null) {
            // Try flexible matching (trim + case insensitive)
            System.out.println("Case-insensitive failed, trying flexible search...");
            user = findByUsernameFlexible(username);
        }

        if (user != null) {
            System.out.println("User found: " + user.getUserName() + " (ID: " + user.getId() + ")");
        } else {
            System.out.println("No user found for: [" + username + "]");
        }

        return user;
    }

    // Add this method for debugging
    public List<String> getAllUsernames() {
        List<UserEntity> allUsers = userRepository.findAll();
        List<String> usernames = allUsers.stream()
                .map(UserEntity::getUserName)
                .collect(Collectors.toList());

        System.out.println("Total users in database: " + usernames.size());
        return usernames;
    }

    // Flexible matching method
    public UserEntity findByUsernameFlexible(String username) {
        String trimmedUsername = username.trim();

        // Try finding all users and manually compare (last resort)
        List<UserEntity> allUsers = userRepository.findAll();
        for (UserEntity u : allUsers) {
            if (u.getUserName() != null &&
                    u.getUserName().trim().equalsIgnoreCase(trimmedUsername)) {
                System.out.println("Found flexible match: [" + u.getUserName() + "] for search: [" + username + "]");
                return u;
            }
        }

        return null;
    }

    // Method to delete user by ID (for rollback scenarios)
    public void deleteById(Long id) {
        System.out.println("Deleting user with ID: " + id);
        userRepository.deleteById(id);
    }

    // Method to check if username exists
    public boolean existsByUsername(String username) {
        return userRepository.findByUserName(username) != null;
    }

    // Method to get user count
    public long getUserCount() {
        return userRepository.count();
    }
}
