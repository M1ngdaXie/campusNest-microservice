package com.campusnest.userservice.services;

import com.campusnest.userservice.models.User;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;


public interface EmailVerificationService {


    void sendVerificationEmail(String email);

    User verifyToken(String token);
}
