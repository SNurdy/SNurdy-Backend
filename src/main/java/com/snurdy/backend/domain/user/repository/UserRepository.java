package com.snurdy.backend.domain.user.repository;

import com.snurdy.backend.domain.user.entity.SocialType;
import com.snurdy.backend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
	Optional<User> findBySocialTypeAndSocialId(SocialType socialType, String socialId);
}