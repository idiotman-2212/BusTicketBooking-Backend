package com.ticketbooking.BusTicketBooking.repository;

import com.ticketbooking.BusTicketBooking.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepo extends JpaRepository <User, Long>{
    @Query(value = """
            select * from user where user.role_id <> (select id from role r where r.role_code = 'ROLE_ADMIN')
            """, nativeQuery = true)
    List<User> findAllExceptAdmin();

    Optional<User> findByUsername(String username);

    Optional<User> findByPhone(String phone);

    Optional<User> findByEmail(String email);

    void deleteByUsername(String username);
}
