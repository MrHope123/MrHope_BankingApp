package com.MrHope123.MrHope_BankingApp.repository;

import com.MrHope123.MrHope_BankingApp.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User,Long> {

     boolean existsByEmail(String email);

     boolean existsByAccountNumber(String accountNumber);

     Optional<User> findByAccountNumber(String accountNumber);

     /**
      * Loads the account with a row lock so concurrent credits, debits and
      * transfers on the same account run one after another instead of
      * overwriting each other's balance.
      */
     @Lock(LockModeType.PESSIMISTIC_WRITE)
     Optional<User> findWithLockByAccountNumber(String accountNumber);

}
