package com.pallet.pallet_login.service;

import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Set;

@Service
public class FaceService {

    private static final int MAX_IMAGE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final Set<String> ALLOWED_IMAGE_FORMATS = Set.of("jpeg", "jpg", "png");

    public boolean registerFace(String base64Image, String userId) throws IOException, InterruptedException {
        System.out.println("Starting face registration for user: " + userId);

        // Validate input
        if (!validateBase64Image(base64Image)) {
            System.out.println("Invalid image format or size for user: " + userId);
            return false;
        }

        // Remove data URL prefix if present
        if (base64Image.contains(",")) {
            base64Image = base64Image.split(",")[1];
        }

        // Validate base64 format and size
        try {
            byte[] imageData = Base64.getDecoder().decode(base64Image);
            if (imageData.length > MAX_IMAGE_SIZE) {
                System.out.println("Image size exceeds maximum allowed size for user: " + userId);
                return false;
            }
            System.out.println("Base64 image validated successfully. Size: " + imageData.length + " bytes");
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid base64 image input for user " + userId + ": " + e.getMessage());
            return false;
        }

        // Create temp file with unique name to avoid conflicts
        String tempFileName = "face_" + userId + "_" + System.currentTimeMillis() + ".txt";
        File tempFile = new File("face-recognition/temp/" + tempFileName);
        tempFile.getParentFile().mkdirs();

        try (Writer writer = new OutputStreamWriter(new FileOutputStream(tempFile), StandardCharsets.UTF_8)) {
            writer.write(base64Image.trim());
        }

        System.out.println("Temp file created: " + tempFile.getAbsolutePath());

        ProcessBuilder builder = new ProcessBuilder(
                "python",
                "face-recognition/register.py",
                tempFile.getAbsolutePath(),
                userId.trim()
        );

        builder.redirectErrorStream(true);
        Process process = builder.start();

        boolean success = false;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("Python Output: " + line);
                // Check for specific success message
                if (line.contains("Embedding saved for user:")) {
                    success = true;
                }
            }
        }

        int exitCode = process.waitFor();

        // Clean up temp file
        if (tempFile.exists()) {
            boolean deleted = tempFile.delete();
            System.out.println("Temp file cleanup: " + (deleted ? "Success" : "Failed"));
        }

        boolean finalResult = exitCode == 0 && success;
        System.out.println("Face registration " + (finalResult ? "SUCCESS" : "FAILED") +
                " for user: " + userId + " (Exit code: " + exitCode + ")");

        return finalResult;
    }

    public String recognizeFaceFromImage(String base64Image) {
        System.out.println("Starting face recognition...");

        try {
            // Validate base64 image format
            if (!validateBase64Image(base64Image)) {
                System.out.println("Invalid image format for recognition");
                return "Unknown";
            }

            // Strip data URL prefix if present
            if (base64Image.contains(",")) {
                base64Image = base64Image.split(",")[1];
            }

            // Decode and save to temp image file
            byte[] imageBytes = Base64.getDecoder().decode(base64Image);
            String imagePath = "face-recognition/temp/recognize.jpg";
            Files.createDirectories(Paths.get("face-recognition/temp"));
            Files.write(Paths.get(imagePath), imageBytes);

            // Use image path as argument
            ProcessBuilder pb = new ProcessBuilder("python", "face-recognition/recognize.py", imagePath);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            String result = "Unknown";

            while ((line = reader.readLine()) != null) {
                System.out.println("Python Output: " + line);
                if (!line.trim().isEmpty() && !line.startsWith("DEBUG:")) {
                    result = line.trim();
                }
            }

            int exitCode = process.waitFor();
            System.out.println("Face recognition completed with exit code: " + exitCode);
            return result;

        } catch (Exception e) {
            System.out.println("Error during face recognition: " + e.getMessage());
            e.printStackTrace();
            return "Unknown";
        }
    }

    private boolean validateBase64Image(String base64Image) {
        if (base64Image == null || base64Image.trim().isEmpty()) {
            System.out.println("Base64 image is null or empty");
            return false;
        }

        // Check for data URL format and extract format
        if (base64Image.startsWith("data:image/")) {
            try {
                String format = base64Image.substring(11, base64Image.indexOf(';'));
                boolean isValidFormat = ALLOWED_IMAGE_FORMATS.contains(format.toLowerCase());
                System.out.println("Image format: " + format + " - Valid: " + isValidFormat);
                return isValidFormat;
            } catch (Exception e) {
                System.out.println("Error parsing data URL format: " + e.getMessage());
                return false;
            }
        }

        // For plain base64, we'll allow it and let Python handle validation
        System.out.println("Plain base64 format detected - allowing");
        return true;
    }
}

