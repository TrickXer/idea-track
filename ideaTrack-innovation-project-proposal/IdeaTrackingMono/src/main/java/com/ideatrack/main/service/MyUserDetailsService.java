package com.ideatrack.main.service;

import com.ideatrack.main.data.Constants;
import com.ideatrack.main.data.User;
import com.ideatrack.main.repository.IUserRepository;
import lombok.NonNull;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class MyUserDetailsService implements UserDetailsService {

    private final IUserRepository userRepository;

    public MyUserDetailsService(IUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(@NonNull String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        boolean enabled = user.getStatus() == Constants.Status.ACTIVE && !user.isDeleted();

        return org.springframework.security.core.userdetails.User.withUsername(user.getEmail())
                .password(user.getPassword())
                .authorities(Collections.singletonList(new SimpleGrantedAuthority(user.getRole().name())))
                .disabled(!enabled)
                .build();
    }
}
