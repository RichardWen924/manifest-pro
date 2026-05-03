package com.manifestreader.model.dto;

import jakarta.validation.constraints.NotNull;

public record FreightDemandAcceptRequest(@NotNull Long quoteId) {
}
