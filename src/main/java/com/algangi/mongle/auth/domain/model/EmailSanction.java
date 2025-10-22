package com.algangi.mongle.auth.domain.model;

import com.algangi.mongle.global.entity.TimeBaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "email_sanction")
@Getter
@Builder(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailSanction extends TimeBaseEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private String email;

    @Column(nullable = false)
    @Builder.Default
    private int hardBounceCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private boolean isBanned = false;

    public static EmailSanction create(String email) {
        return EmailSanction.builder()
            .email(email)
            .build();
    }

    public void incrementHardBounceCount() {
        this.hardBounceCount += 1;
    }

    public void markAsBanned() {
        this.isBanned = true;
    }
}