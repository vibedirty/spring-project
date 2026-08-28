package com.cat.hard.integration.product.dto;

public record ProductApiResponse<T>(int code, String message, T data) {
}
