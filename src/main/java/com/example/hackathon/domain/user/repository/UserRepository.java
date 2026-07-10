package com.example.hackathon.domain.user.repository;

import com.example.hackathon.domain.user.entity.Provider;
import com.example.hackathon.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByProviderAndProviderId(Provider provider, String providerId);
}
