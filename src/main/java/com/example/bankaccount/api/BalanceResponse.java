package com.example.bankaccount.api;

import java.math.BigDecimal;

/**
 * Response body for a balance enquiry, e.g. {@code {"balance": 12345.67, "currency": "GBP"}}.
 *
 * @param balance  the balance in pounds, with two decimal places
 * @param currency currency code
 */
public record BalanceResponse(BigDecimal balance, String currency) {
}
