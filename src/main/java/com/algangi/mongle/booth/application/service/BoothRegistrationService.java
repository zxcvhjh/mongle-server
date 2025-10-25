package com.algangi.mongle.booth.application.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.algangi.mongle.booth.presentation.dto.BoothRegistrationRequest;
import com.algangi.mongle.booth.presentation.dto.BoothRegistrationResponse;
import com.algangi.mongle.member.application.service.MemberFinder;
import com.algangi.mongle.member.domain.model.Member;
import com.algangi.mongle.member.domain.repository.MemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BoothRegistrationService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final MemberFinder memberFinder;

    private static String generateBoothEmail(String boothName) {
        return String.format("booth_%s@mongle.com", boothName);
    }

    private static String generateBoothId(String boothName) {
        return "booth_" + boothName;
    }

    @Transactional
    public BoothRegistrationResponse registerBooth(BoothRegistrationRequest request) {
        String boothName = request.boothName();
        String boothId = generateBoothId(boothName);
        String boothEmail = generateBoothEmail(request.boothName());
        String encodedPassword = passwordEncoder.encode(request.password());

        memberFinder.validateDuplicateEmail(boothEmail);
        memberFinder.validateDuplicateNickName(boothName);

        Member boothAccount = Member.createUserWithId(boothId, boothEmail, encodedPassword,
            boothName,
            null);
        Member savedBooth = memberRepository.save(boothAccount);
        return BoothRegistrationResponse.of(savedBooth, request.password());
    }

}
