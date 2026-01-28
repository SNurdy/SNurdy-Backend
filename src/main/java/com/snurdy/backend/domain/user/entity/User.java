package com.snurdy.backend.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

import com.snurdy.backend.domain.auth.oauth.dto.SocialUserInfo;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
	name = "users",
	uniqueConstraints = {
		@UniqueConstraint(name = "uk_user_social_id_type", columnNames = {"socialId", "socialType"})
	}
)
public class User {

	@Id
	@Column(columnDefinition = "BINARY(16)")
	private UUID id;

	@Column(nullable = false)
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

	@Builder.Default
	private int currentStreak = 0;

	@Builder.Default
	private int totalLearningTerms = 0;

	@Builder.Default
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