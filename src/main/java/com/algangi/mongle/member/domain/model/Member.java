package com.algangi.mongle.member.domain.model;

import com.algangi.mongle.global.entity.TimeBaseEntity;
import com.github.f4b6a3.ulid.UlidCreator;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;


@Entity
@Table(name = "member", uniqueConstraints = {
    @UniqueConstraint(name = "uk_member_email", columnNames = "email"),
    @UniqueConstraint(name = "uk_member_nickname", columnNames = "nickname")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
@Getter
public class Member extends TimeBaseEntity {

    @Id
    String memberId;
    @Column(nullable = false, updatable = false)
    String email;
    @Column(nullable = false)
    String nickname;
    String profileImage;
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    MemberRole memberRole;
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    MemberStatus status = MemberStatus.ACTIVE;
    @Column(nullable = false)
    private String encodedPassword;
    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SocialAccount> socialAccounts = new ArrayList<>();

    public static Member createUser(String email, String encodedPassword, String nickname,
        String profileImage) {
        validateUserEssentials(email, encodedPassword, nickname);
        return Member.builder()
            .memberId(UlidCreator.getUlid().toString())
            .email(email)
            .encodedPassword(encodedPassword)
            .nickname(nickname)
            .profileImage(profileImage)
            .memberRole(MemberRole.USER)
            .build();
    }

    public static Member createBoothAccount(String memberId, String email, String encodedPassword,
        String nickname, String profileImage) {
        validateMemberId(memberId);
        validateUserEssentials(email, encodedPassword, nickname);
        return Member.builder()
            .memberId(memberId)
            .email(email)
            .encodedPassword(encodedPassword)
            .nickname(nickname)
            .profileImage(profileImage)
            .memberRole(MemberRole.BOOTH)
            .build();
    }

    private static void validateMemberId(String memberId) {
        if (!StringUtils.hasText(memberId)) {
            throw new IllegalArgumentException("memberId는 필수값입니다.");
        }
    }

    public static Member createAdmin(String memberId, String email, String nickname,
        String profileImage, String encodedPassword) {
        validateMemberId(memberId);
        validateUserEssentials(email, encodedPassword, nickname);
        return Member.builder()
            .memberId(memberId)
            .email(email)
            .nickname(nickname)
            .profileImage(profileImage)
            .memberRole(MemberRole.ADMIN)
            .encodedPassword(encodedPassword)
            .build();
    }

    private static void validateUserEssentials(String email, String password, String nickname) {
        if (!StringUtils.hasText(email)) {
            throw new IllegalArgumentException("이메일은 필수값입니다.");
        }
        if (!StringUtils.hasText(password)) {
            throw new IllegalArgumentException("비밀번호는 필수값입니다.");
        }
        if (!StringUtils.hasText(nickname)) {
            throw new IllegalArgumentException("닉네임은 필수값입니다");
        }
    }

    public void addSocialAccount(SocialAccount socialAccount) {
        socialAccounts.add(socialAccount);
        socialAccount.setMember(this);
    }

    public void ban() {
        this.status = MemberStatus.BANNED;
    }

    public void deactivate() {
        this.status = MemberStatus.DEACTIVATED;
    }

    public void updateProfileImage(String profileImageKey) {
        this.profileImage = profileImageKey;
    }

    public void updateNickname(String newNickname) {
        if (newNickname == null || newNickname.isBlank()) {
            throw new IllegalArgumentException("닉네임은 비워둘 수 없습니다.");
        }
        this.nickname = newNickname;
    }


    public boolean isAdmin() {
        return this.memberRole == MemberRole.ADMIN;
    }

    public boolean isBooth() {
        return this.memberRole == MemberRole.BOOTH;
    }

    /**
     * 회원의 활성 상태를 검증합니다.
     * BANNED 또는 DEACTIVATED 상태일 경우 예외를 발생시킵니다.
     *
     * @throws com.algangi.mongle.global.exception.ApplicationException 회원이 차단되었거나 탈퇴한 경우
     */
    public void validateActive() {
        if (this.status == MemberStatus.BANNED) {
            throw new com.algangi.mongle.global.exception.ApplicationException(
                com.algangi.mongle.member.exception.MemberErrorCode.MEMBER_IS_BANNED);
        }
        if (this.status == MemberStatus.DEACTIVATED) {
            throw new com.algangi.mongle.global.exception.ApplicationException(
                com.algangi.mongle.member.exception.MemberErrorCode.MEMBER_IS_DEACTIVATED);
        }
    }
}
