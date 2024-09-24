package com.ticketbooking.repo;

import com.ticketbooking.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
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

    @Modifying
    @Query("UPDATE User u SET u.loyaltyPoints = u.loyaltyPoints + :points WHERE u.username = :username")
    void addLoyaltyPoints(@Param("username") String username, @Param("points") BigDecimal points);

    @Modifying
    @Query("UPDATE User u SET u.loyaltyPoints = u.loyaltyPoints - :points WHERE u.username = :username")
    void deductLoyaltyPoints(@Param("username") String username, @Param("points") BigDecimal points);
}
