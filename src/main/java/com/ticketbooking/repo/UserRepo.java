package com.ticketbooking.repo;

import com.ticketbooking.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepo extends JpaRepository<User, String> {

    @Query(value = """
            select * from user where user.role_id <> (select id from role r where r.role_code = 'ROLE_ADMIN')
            """, nativeQuery = true)
    List<User> findAllExceptAdmin();

    Optional<User> findByUsername(String username);

    Optional<User> findByPhone(String phone);

    Optional<User> findByEmail(String email);

    void deleteByUsername(String username);
}
