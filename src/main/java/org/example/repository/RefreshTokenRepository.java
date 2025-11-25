package org.example.repository;

import org.example.model.RefreshToken;
import org.example.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    Optional<RefreshToken> findByUserId(Long userId);
    int deleteByUserId(Long userId);

    void deleteByUser(User user);

    Optional<RefreshToken> findByUser(User user);
}
