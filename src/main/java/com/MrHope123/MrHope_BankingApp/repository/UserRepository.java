package com.MrHope123.MrHope_BankingApp.repository;

import com.MrHope123.MrHope_BankingApp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User,Long> {

     Boolean existsByEmail(String email);

}
