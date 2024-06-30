package com.ticketbooking.BusTicketBooking.repository;

import com.ticketbooking.BusTicketBooking.entity.User;
import com.ticketbooking.BusTicketBooking.entity.UserPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserPermissionRepo extends JpaRepository<UserPermission, Long> {
    List<UserPermission> findAllByUser(User username);

    @Modifying
    @Query(value = "delete " +
            "from user_permission up where up.screen_code=:screenCode "+
            "and username=:username " +
            "and up.role_id not in (select id from role r where r.role_code in ('ROLE_ADMIN', 'ROLE_STAFF', 'ROLE_CUSTOMER'))"
            , nativeQuery = true)
    void deleteOldUserCrudRoles(@Param("screenCode") String screenCode,
                                @Param("username") String username);
}
