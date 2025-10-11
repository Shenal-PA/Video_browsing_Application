package com.example.videobrowsing.service;

import com.example.videobrowsing.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserService userService;

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        // Try to find user by username first
        Optional<User> userOpt = userService.findByUsername(usernameOrEmail);
        
        // If not found by username, try email
        if (userOpt.isEmpty()) {
            userOpt = userService.findByEmail(usernameOrEmail);
        }
        
        // If still not found, throw exception
        if (userOpt.isEmpty() || !userOpt.get().getIsActive()) {
            throw new UsernameNotFoundException("User not found or inactive: " + usernameOrEmail);
        }
        
        User user = userOpt.get();
        
        // Create authorities based on user role
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        if (user.getRole() != null) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
        }
        
        // Create Spring Security UserDetails object
        return new org.springframework.security.core.userdetails.User(
            user.getUsername(), 
            user.getPassword(), 
            user.getIsActive(), 
            true, // account non-expired
            true, // credentials non-expired
            true, // account non-locked
            authorities
        );
    }
}
