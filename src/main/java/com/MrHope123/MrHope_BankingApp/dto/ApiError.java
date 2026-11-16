package com.MrHope123.MrHope_BankingApp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/** Error body returned for every failed request. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(int status, String responseCode, String responseMessage, Map<String, String> errors) {
}
