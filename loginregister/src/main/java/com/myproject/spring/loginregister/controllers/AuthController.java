package com.myproject.spring.loginregister.controllers;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.myproject.spring.loginregister.models.ERole;
import com.myproject.spring.loginregister.models.Role;
import com.myproject.spring.loginregister.models.User;
import com.myproject.spring.loginregister.payload.request.LoginRequest;
import com.myproject.spring.loginregister.payload.request.RegisterRequest;
import com.myproject.spring.loginregister.payload.response.JwtResponse;
import com.myproject.spring.loginregister.payload.response.MessageResponse;
import com.myproject.spring.loginregister.repositories.RoleRepository;
import com.myproject.spring.loginregister.repositories.UserRepository;
import com.myproject.spring.loginregister.security.jwt.JwtUtils;
import com.myproject.spring.loginregister.security.services.UserDetailsImpl;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
	AuthenticationManager authenticationManager;
    
    @Autowired
    UserRepository userRepository;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    JwtUtils jwtUtils;
    
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
    	Authentication authentication =
    	  authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

    	SecurityContextHolder.getContext().setAuthentication(authentication);
    	String jwt = jwtUtils.generateJwtToken(authentication);
    	
    	UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
    	List<String> roles = userDetails.getAuthorities().stream()
    		.map(item->item.getAuthority())	
    		.collect(Collectors.toList());
    	
    	return ResponseEntity.ok(new JwtResponse(jwt, 
    			userDetails.getId(), 
    			userDetails.getUsername(), 
    			userDetails.getEmail(), 
    			roles));
    }
    
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
    	if (userRepository.existsByUsername(registerRequest.getUsername())) {
    		return ResponseEntity
    			.badRequest()
    			.body(new MessageResponse("Error: Username is already taken!"));
    	}
    	
    	if (userRepository.existsByEmail(registerRequest.getEmail())) {
    		return ResponseEntity
    			.badRequest()
    			.body(new MessageResponse("Error: Email is already in use!"));
    	}
    	
    	//Create new user's account
    	User user = new User(registerRequest.getUsername(),
    			  registerRequest.getEmail(),
    			  encoder.encode(registerRequest.getPassword()));
    	
    	Set<String> strRoles = registerRequest.getRole();
    	Set<Role> roles = new HashSet<>();
    	
    	if (strRoles == null) {
    		Role userRole = roleRepository.findByName(ERole.ROLE_USER)
    			.orElseThrow(()->new RuntimeException("Error: Role is not found."));
    		roles.add(userRole);
    	} else {
    		strRoles.forEach(role->{
    		  switch (role) {
    		  case "admin":
    			Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN)
    			    .orElseThrow(()->new RuntimeException("Error: Role is not found."));
    			roles.add(adminRole);
    			
    			break;
    			
    		  case "mod":
    			Role modRole = roleRepository.findByName(ERole.ROLE_MODERATOR)
    			    .orElseThrow(()->new RuntimeException("Error: Role is not found."));
    			roles.add(modRole);
    			
    			break;
    		  default:
    			  Role userRole = roleRepository.findByName(ERole.ROLE_USER)
    			      .orElseThrow(()->new RuntimeException("Error: Role is not found."));
    			  roles.add(userRole);
    		  }
    		});
    	}
    	
    	user.setRoles(roles);
    	userRepository.save(user);
    	
    	return ResponseEntity.ok(new MessageResponse("User registered successfully"));
    }
}
