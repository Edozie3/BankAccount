package com.example.bankaccount.api;

import com.example.bankaccount.service.BankAccountService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API for balance enquiries.
 */
@RestController
@RequestMapping("/api/v1/account")
public class BalanceController {

    private static final String CURRENCY = "GBP";

    private final BankAccountService bankAccountService;

    public BalanceController(BankAccountService bankAccountService) {
        this.bankAccountService = bankAccountService;
    }

    @GetMapping("/balance")
    public BalanceResponse getBalance() {
        BigDecimal balance = BigDecimal.valueOf(bankAccountService.retrieveBalance())
                .setScale(2, RoundingMode.HALF_EVEN);
        return new BalanceResponse(balance, CURRENCY);
    }
}
