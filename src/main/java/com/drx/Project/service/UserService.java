package com.drx.Project.service;

import com.drx.Project.model.User;
import com.drx.Project.repositories.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class UserService {

    private final EntityManager entityManager;
    private final UserRepository userRepository;

    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public List<User> getAllUsers(){
        return userRepository.findAll();
    }

    public Optional<User> getUserById(int id){
        return userRepository.findById(id);
    }

    public void saveToDb(User user){
        userRepository.save(user);
    }


    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Transactional
    public void deleteUserAndRoles(int id) {
        // delete from user_roles
        String deleteUserRolesQuery = "DELETE FROM user_roles WHERE user_id = :id";
        entityManager.createNativeQuery(deleteUserRolesQuery)
                .setParameter("id", id)
                .executeUpdate();

        String nullifyUserInLogsQuery = "UPDATE mylog SET user_id = NULL WHERE user_id = :id";
        entityManager.createNativeQuery(nullifyUserInLogsQuery)
                .setParameter("id", id)
                .executeUpdate();

        userRepository.deleteById(id);
    }
}