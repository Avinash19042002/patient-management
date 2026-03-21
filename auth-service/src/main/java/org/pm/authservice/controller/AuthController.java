package org.pm.authservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.pm.authservice.dto.LoginRequestDTO;
import org.pm.authservice.dto.LoginResponseDTO;
import org.pm.authservice.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Generate token on user login")
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(
            @RequestBody @Valid LoginRequestDTO loginRequestDTO) {
        Optional<String> tokenOptional = authService.authenticate(loginRequestDTO);

        if (tokenOptional.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        String token = tokenOptional.get();
        return new ResponseEntity<>(new LoginResponseDTO(token), HttpStatus.OK);
    }

    @Operation(summary = "Validate Token")
    @GetMapping("/validate")
    public ResponseEntity<Void> validateToken(
            @RequestHeader("Authorization") String authHeader){
         // Authorization: Bearer <token>
        if(authHeader==null || !authHeader.startsWith("Bearer ")){
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        return authService.validateToken(authHeader.substring(7))
                ? ResponseEntity.ok().build()
                : ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
}
