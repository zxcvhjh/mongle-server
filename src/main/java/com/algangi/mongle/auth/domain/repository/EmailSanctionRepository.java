package com.algangi.mongle.auth.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.algangi.mongle.auth.domain.model.EmailSanction;

public interface EmailSanctionRepository extends JpaRepository<EmailSanction, String> {

}