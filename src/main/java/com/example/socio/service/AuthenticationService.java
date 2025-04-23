package com.example.socio.service;

import com.example.socio.entity.User;
import com.example.socio.model.AuthenticationResponse;
import com.example.socio.model.LoginRequest;
import com.example.socio.model.RegisterRequest;
import com.example.socio.model.ResetPasswordRequest;
import com.example.socio.repository.UserRepository;
import com.example.socio.secuirty.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
@Service
public class AuthenticationService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Transactional
    public void registerUser(RegisterRequest registerRequest) {
        if (userRepository.findByEmail(registerRequest.getEmail()).isPresent()) {
            throw new RuntimeException("User already registered");
        }
        User user = User.builder()
                .name(registerRequest.getName())
                .email(registerRequest.getEmail())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .dob(registerRequest.getDob())
                .isPrivate(registerRequest.isPrivate())
                .passwordLastUpdated(new Date())
                .build();
        userRepository.save(user);
    }

   public AuthenticationResponse authenticateUser(LoginRequest loginRequest) {
       try {
           Authentication authentication = authenticationManager.authenticate(
                   new UsernamePasswordAuthenticationToken(
                           loginRequest.getEmail(),
                           loginRequest.getPassword()
                   )
           );
           User user = userRepository.findByEmail(authentication.getName())
                   .orElseThrow(() -> new RuntimeException("User not found"));

           // Check if the password is expired
           if (isPasswordExpired(user.getPasswordLastUpdated())) {
               throw new RuntimeException("Password expired. Please reset your password.");
           }

           String token = jwtTokenProvider.generateToken(user);
           return new AuthenticationResponse(token, user.getRole().name());
       } catch (BadCredentialsException e) {
           throw new RuntimeException("Invalid email or password");
       }
   }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordLastUpdated(new Date());
        userRepository.save(user);
    }

    private boolean isPasswordExpired(Date lastUpdated) {
        if (lastUpdated == null) {
            // Treat null as expired or handle it based on your business logic
            return true;
        }

        long thirtyDaysInMillis = 30L * 24 * 60 * 60 * 1000;
        return new Date().getTime() - lastUpdated.getTime() > thirtyDaysInMillis;
    }
}