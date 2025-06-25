package br.ifsp.my_movinhos.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import br.ifsp.my_movinhos.dto.JwtResponse;
import br.ifsp.my_movinhos.dto.LoginRequest;
import br.ifsp.my_movinhos.model.User;
import br.ifsp.my_movinhos.security.JwtService;
import br.ifsp.my_movinhos.security.UserDetailService;

@RestController
@RequestMapping("/my-movinhos/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtService tokenProvider;

    @PostMapping("/login")
    public ResponseEntity<JwtResponse> authenticateUser(@RequestBody LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                loginRequest.getUsername(),
                loginRequest.getPassword()
            )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        User user = (User) authentication.getPrincipal();
        String jwt = tokenProvider.generateToken(user);

        return ResponseEntity.ok(new JwtResponse(jwt, "Bearer"));
    }
}
