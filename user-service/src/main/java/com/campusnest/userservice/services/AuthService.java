package com.campusnest.userservice.services;

import com.campusnest.userservice.requests.ForgotPasswordRequest;
import com.campusnest.userservice.requests.LoginRequest;
import com.campusnest.userservice.requests.LogoutRequest;
import com.campusnest.userservice.requests.RefreshTokenRequest;
import com.campusnest.userservice.requests.ResetPasswordRequest;
import com.campusnest.userservice.response.ForgotPasswordResponse;
import com.campusnest.userservice.response.LoginResponse;
import com.campusnest.userservice.requests.RegisterRequest;
import com.campusnest.userservice.response.LogoutResponse;
import com.campusnest.userservice.response.RefreshTokenResponse;
import com.campusnest.userservice.response.RegisterResponse;
import com.campusnest.userservice.response.ResetPasswordResponse;

public interface AuthService {

    RegisterResponse registerUser(RegisterRequest request);

    LoginResponse login(LoginRequest request);

    RefreshTokenResponse refreshAccessToken(RefreshTokenRequest request);
    
    LogoutResponse logout(LogoutRequest request);
    
    ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request, String ipAddress, String userAgent);
    
    ResetPasswordResponse resetPassword(ResetPasswordRequest request);
}
