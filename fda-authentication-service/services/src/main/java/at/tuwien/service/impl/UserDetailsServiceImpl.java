package at.tuwien.service.impl;

import at.tuwien.api.user.UserDetailsDto;
import at.tuwien.entities.user.User;
import at.tuwien.exception.UserNotFoundException;
import at.tuwien.mapper.UserMapper;
import at.tuwien.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Slf4j
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserService userService;
    private final UserMapper userMapper;

    @Autowired
    public UserDetailsServiceImpl(UserService userService, UserMapper userMapper) {
        this.userService = userService;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        final User user;
        try {
            user = userService.findByUsername(username);
        } catch (UserNotFoundException e) {
            log.error("Failed to find user with username {}", username);
            throw new UsernameNotFoundException("Failed to find user", e);
        }
        log.trace("loaded user {}", user);
        final UserDetailsDto details = userMapper.userToUserDetailsDto(user);
        details.setAuthorities(user.getRoles()
                .stream()
                .map(userMapper::roleTypeToGrantedAuthority)
                .collect(Collectors.toList()));
        log.trace("mapped user {}", details);
        return details;
    }

}
