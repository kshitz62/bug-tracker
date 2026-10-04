package com.bugtracker.repository;

import com.bugtracker.entity.RoleName;
import com.bugtracker.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    /**
     * @return all users holding the supplied role (used to populate assignee drop downs).
     */
    @Query("""
            select distinct u from User u
            join u.roles r
            where r.name = :roleName
            order by u.fullName asc
            """)
    List<User> findAllByRoleName(@Param("roleName") RoleName roleName);

    @Query("""
            select distinct u from User u
            where u.id in (
                select b.assignedDeveloper.id from Bug b where b.assignedDeveloper is not null
            )
            order by u.fullName asc
            """)
    List<User> findUsersWithAssignedBugs();
}
