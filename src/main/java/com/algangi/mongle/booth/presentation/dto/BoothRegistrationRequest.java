package com.algangi.mongle.booth.presentation.dto;

import jakarta.validation.constraints.NotBlank;

public record BoothRegistrationRequest(
    @NotBlank
    String boothName,
    @NotBlank
    String password
) {

}
