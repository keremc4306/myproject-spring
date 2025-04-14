package com.myproject.spring.loginregister.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.myproject.spring.loginregister.models.ERole;
import com.myproject.spring.loginregister.models.Role;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long>{
    Optional<Role> findByName(ERole name);
}
