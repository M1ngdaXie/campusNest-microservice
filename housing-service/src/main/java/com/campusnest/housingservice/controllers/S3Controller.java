package com.campusnest.housingservice.controllers;

import com.campusnest.housingservice.services.S3Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/images")
public class S3Controller {

    @Autowired
    private S3Service s3Service;

    @PostMapping("/upload-url")
    public ResponseEntity<Map<String, String>> generateUploadUrl(
            @RequestParam String fileName,
            @RequestParam String contentType) {
        
        String presignedUrl = s3Service.generatePresignedUploadUrl(fileName, contentType);
        String s3Key = s3Service.extractS3KeyFromUrl(presignedUrl);
        
        Map<String, String> response = new HashMap<>();
        response.put("uploadUrl", presignedUrl);
        response.put("s3Key", s3Key);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/url/**")
    public ResponseEntity<Map<String, String>> getImageUrl(HttpServletRequest request) {
        String s3Key = request.getRequestURI().substring("/api/images/url/".length());
        String imageUrl = s3Service.getImageUrl(s3Key);
        
        Map<String, String> response = new HashMap<>();
        response.put("imageUrl", imageUrl);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/test-basic")
    public ResponseEntity<Map<String, String>> testBasic() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Controller is working");
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/test-connection")
    public ResponseEntity<Map<String, String>> testS3Connection() {
        Map<String, String> response = new HashMap<>();
        try {
            System.out.println("Testing S3 connection...");
            String testUrl = s3Service.generatePresignedUploadUrl("test.txt", "text/plain");
            System.out.println("S3 URL generated successfully: " + testUrl);
            
            response.put("status", "success");
            response.put("message", "S3 connection working");
            response.put("testUrl", testUrl);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("S3 Error: " + e.getMessage());
            e.printStackTrace();
            
            response.put("status", "error");
            response.put("message", e.getMessage());
            response.put("errorType", e.getClass().getSimpleName());
            
            return ResponseEntity.status(500).body(response);
        }
    }
}
