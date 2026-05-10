package com.inncontrol.service;

import com.inncontrol.dto.AuthRequest;
import com.inncontrol.dto.AuthResponse;
import com.inncontrol.dto.RegisterRequest;
import com.inncontrol.model.User;
import com.inncontrol.repository.UserRepository;
import com.inncontrol.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Este correo ya está registrado");
        }
        // Safety check for photo size
        String photo = request.getPhoto();
        if (photo != null && photo.length() > 1024 * 1024) {
            throw new RuntimeException("La imagen es demasiado grande (máximo 1MB)");
        }

        var user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .photo(photo)
                .build();
        userRepository.save(user);
        var jwtToken = jwtService.generateToken(user);
        return AuthResponse.builder()
                .id(user.getId())
                .token(jwtToken)
                .name(user.getName())
                .role(user.getRole().name())
                .photo(user.getPhoto())
                .build();
    }

    public AuthResponse authenticate(AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );
        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow();
        var jwtToken = jwtService.generateToken(user);
        return AuthResponse.builder()
                .id(user.getId())
                .token(jwtToken)
                .name(user.getName())
                .role(user.getRole().name())
                .photo(user.getPhoto())
                .build();
    }

    @Transactional
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    @Transactional
    public User updateUserAdmin(Long id, RegisterRequest request) {
        var user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setRole(request.getRole());
        
        // Safety check for photo size
        String photo = request.getPhoto();
        if (photo != null && photo.length() > 1024 * 1024) {
            throw new RuntimeException("La imagen es demasiado grande (máximo 1MB)");
        }
        user.setPhoto(photo);
        
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        
        return userRepository.save(user);
    }

    @Transactional
    public AuthResponse updateProfile(String email, com.inncontrol.dto.UserUpdateRequest request) {
        var user = userRepository.findByEmail(email).orElseThrow();
        if (request.getName() != null) user.setName(request.getName());
        
        // Safety check for photo size
        String photo = request.getPhoto();
        if (photo != null && photo.length() > 1024 * 1024) {
            throw new RuntimeException("La imagen es demasiado grande (máximo 1MB)");
        }
        user.setPhoto(photo);
        
        userRepository.save(user);
        
        return AuthResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .photo(user.getPhoto())
                .build();
    }
}
