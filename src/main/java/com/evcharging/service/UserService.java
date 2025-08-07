package com.evcharging.service;

import com.evcharging.model.User;
import java.util.List;
import java.util.Optional;

public interface UserService {
    User saveUser(User user);
    List<User> getAllUsers();
    Optional<User> getUserByUsername(String username);
    User getUserById(Long id);
    void deleteUser(Long id);
     User getUserByEmail(String email);
     void deleteUserById(Long id);
	User findByEmail(String email);
	User findByUsername(String username);
}
