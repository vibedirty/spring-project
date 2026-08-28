package com.cat.hard.integration.account.dto;

public record AccountApiResponse<T>(int code, String message, T data) {
}
