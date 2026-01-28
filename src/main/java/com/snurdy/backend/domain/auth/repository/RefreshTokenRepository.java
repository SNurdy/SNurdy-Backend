package com.snurdy.backend.domain.auth.repository;

import com.snurdy.backend.domain.auth.entity.RefreshToken;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends CrudRepository<RefreshToken, UUID> {
	Optional<RefreshToken> findByToken(String token);
	void deleteByUserId(UUID userId);
}