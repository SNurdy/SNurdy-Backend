package com.snurdy.backend.domain.auth.controller;

import com.snurdy.backend.domain.auth.dto.request.KakaoLoginRequest;
import com.snurdy.backend.domain.auth.dto.response.TokenResponse;
import com.snurdy.backend.domain.auth.service.AuthService;
import com.snurdy.backend.domain.user.entity.SocialType;
import com.snurdy.backend.global.common.constants.SuccessType;
import com.snurdy.backend.global.common.dto.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	@PostMapping("/kakao")
	public ApiResponse<TokenResponse> kakaoLogin(@RequestBody KakaoLoginRequest request) {
		TokenResponse token = authService.socialLogin(SocialType.KAKAO, request.authorizationCode());
		return ApiResponse.success(SuccessType.LOGIN_SUCCESS, token);
	}

	//    @PostMapping("/merge")
	//    public ApiResponse<Void> mergeData(
	//        @AuthenticationPrincipal UUID userId,
	//        @RequestBody AccountMergeRequest request
	//    ) {
	//        authService.mergeUserAccount(userId, request.localUuid());
	//        return ApiResponse.success(SuccessType.MERGE_SUCCESS);
	//    }
}