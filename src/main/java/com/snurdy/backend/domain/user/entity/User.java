package com.snurdy.backend.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

import com.snurdy.backend.domain.auth.client.SocialUserInfo;

@Entity
@Table(name = "users")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

	@Id
	@Column(columnDefinition = "BINARY(16)")
	private UUID id;

	@Column(nullable = false, unique = true)
	private String socialId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private SocialType socialType;

	private String email;

	@Column(nullable = false, length = 20)
	private String nickname;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Role role;

	private int currentStreak = 0;
	private int totalLearningTerms = 0;
	private int longestStreak = 0;

	public static User from(SocialUserInfo info, SocialType type) {
		User user = new User();
		user.id = UUID.randomUUID();
		user.socialId = info.socialId();
		user.socialType = type;
		user.email = info.email();
		user.role = Role.USER;

		String name = (info.nickname() == null) ? "SNurdy_" + user.id.toString().substring(0, 5) : info.nickname();
		user.nickname = name.length() > 20 ? name.substring(0, 20) : name;

		return user;
	}

	public void recordLearning() {
		this.currentStreak++;
		this.totalLearningTerms++;
		if (this.currentStreak > this.longestStreak) {
			this.longestStreak = this.currentStreak;
		}
	}
}