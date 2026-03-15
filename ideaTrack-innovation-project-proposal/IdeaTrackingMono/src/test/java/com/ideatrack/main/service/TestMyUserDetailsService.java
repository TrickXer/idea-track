package com.ideatrack.main.service;

import com.ideatrack.main.data.Constants;
import com.ideatrack.main.data.User;
import com.ideatrack.main.repository.IUserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TestMyUserDetailsService {

    @Mock
    private IUserRepository userRepository;

    @InjectMocks
    private MyUserDetailsService service;

    // ------------------------------------------------------------
    // loadUserByUsername
    // ------------------------------------------------------------

    @Test
    @DisplayName("loadUserByUsername: returns enabled user when ACTIVE and not deleted")
    void loadUser_ok_enabled() {
        User u = new User();
        u.setEmail("emp@company.com");
        u.setPassword("$bcrypt");
        u.setRole(Constants.Role.EMPLOYEE);
        u.setStatus(Constants.Status.ACTIVE);
        u.setDeleted(false);

        when(userRepository.findByEmail("emp@company.com")).thenReturn(Optional.of(u));

        UserDetails details = service.loadUserByUsername("emp@company.com");

        assertThat(details.getUsername()).isEqualTo("emp@company.com");
        assertThat(details.getPassword()).isEqualTo("$bcrypt");
        assertThat(details.isEnabled()).isTrue();

        assertThat(details.getAuthorities())
                .extracting(a -> a.getAuthority())
                .containsExactly("EMPLOYEE");

        verify(userRepository).findByEmail("emp@company.com");
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    @DisplayName("loadUserByUsername: returns disabled user when INACTIVE")
    void loadUser_disabled_whenInactive() {
        User u = new User();
        u.setEmail("emp@company.com");
        u.setPassword("$bcrypt");
        u.setRole(Constants.Role.EMPLOYEE);
        u.setStatus(Constants.Status.INACTIVE);
        u.setDeleted(false);

        when(userRepository.findByEmail("emp@company.com")).thenReturn(Optional.of(u));

        UserDetails details = service.loadUserByUsername("emp@company.com");

        // because service sets .disabled(!enabled)
        assertThat(details.isEnabled()).isFalse();

        assertThat(details.getAuthorities())
                .extracting(a -> a.getAuthority())
                .containsExactly("EMPLOYEE");

        verify(userRepository).findByEmail("emp@company.com");
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    @DisplayName("loadUserByUsername: returns disabled user when deleted=true even if ACTIVE")
    void loadUser_disabled_whenDeleted() {
        User u = new User();
        u.setEmail("emp@company.com");
        u.setPassword("$bcrypt");
        u.setRole(Constants.Role.EMPLOYEE);
        u.setStatus(Constants.Status.ACTIVE);
        u.setDeleted(true);

        when(userRepository.findByEmail("emp@company.com")).thenReturn(Optional.of(u));

        UserDetails details = service.loadUserByUsername("emp@company.com");

        assertThat(details.isEnabled()).isFalse();

        assertThat(details.getAuthorities())
                .extracting(a -> a.getAuthority())
                .containsExactly("EMPLOYEE");

        verify(userRepository).findByEmail("emp@company.com");
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    @DisplayName("loadUserByUsername: throws UsernameNotFoundException when user not found")
    void loadUser_notFound() {
        when(userRepository.findByEmail("missing@company.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("missing@company.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found: missing@company.com");

        verify(userRepository).findByEmail("missing@company.com");
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    @DisplayName("loadUserByUsername: throws exception when email is null (due to @NonNull)")
    void loadUser_nullEmail_throws() {
        // @NonNull will throw NullPointerException before repository call
        assertThatThrownBy(() -> service.loadUserByUsername(null))
                .isInstanceOf(NullPointerException.class);

        verifyNoInteractions(userRepository);
    }
}