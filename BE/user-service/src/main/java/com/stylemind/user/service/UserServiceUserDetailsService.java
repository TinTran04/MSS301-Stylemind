package com.stylemind.user.service;

import com.stylemind.common.security.UserPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserServiceUserDetailsService implements UserDetailsService {

    @Override
    public UserDetails loadUserByUsername(String subject) throws UsernameNotFoundException {
        return new UserPrincipal(subject, subject, "", "CUSTOMER", "JWT", true);
    }
}
