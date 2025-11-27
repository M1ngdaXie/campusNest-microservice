package com.campusnest.userservice.repository;

import com.campusnest.userservice.models.PasswordResetToken;
import com.campusnest.userservice.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    
    Optional<PasswordResetToken> findByToken(String token);
    
    List<PasswordResetToken> findByUser(User user);
    
    @Query("SELECT prt FROM PasswordResetToken prt WHERE prt.user = :user AND prt.usedAt IS NULL AND prt.expiryDate > :now")
    List<PasswordResetToken> findActiveTokensByUser(@Param("user") User user, @Param("now") Instant now);
    
    @Modifying
    @Query("DELETE FROM PasswordResetToken prt WHERE prt.user = :user")
    void deleteByUser(@Param("user") User user);
    
    @Modifying
    @Query("DELETE FROM PasswordResetToken prt WHERE prt.user = :user AND prt.usedAt IS NULL AND prt.expiryDate > :now")
    void deleteUnusedTokensByUser(@Param("user") User user, @Param("now") Instant now);
    
    @Modifying
    @Query("DELETE FROM PasswordResetToken prt WHERE prt.expiryDate < :now")
    void deleteExpiredTokens(@Param("now") Instant now);
    
    @Query("SELECT COUNT(prt) FROM PasswordResetToken prt WHERE prt.user = :user AND prt.createdAt > :since")
    long countRecentTokensByUser(@Param("user") User user, @Param("since") Instant since);
    
    @Query("SELECT COUNT(prt) FROM PasswordResetToken prt WHERE prt.ipAddress = :ipAddress AND prt.createdAt > :since")
    long countRecentTokensByIpAddress(@Param("ipAddress") String ipAddress, @Param("since") Instant since);
}