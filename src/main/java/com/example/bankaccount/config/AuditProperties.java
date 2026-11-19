package com.example.bankaccount.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Audit settings.
 *
 * @param submissionSize    number of transactions in each audit submission
 * @param queueCapacity     maximum number of transactions waiting to be audited before producers are made to wait
 * @param batchLimitInPence maximum absolute value of a single batch, in pence (£1,000,000 by default)
 */
@ConfigurationProperties("audit")
public record AuditProperties(
        @DefaultValue("1000") int submissionSize,
        @DefaultValue("50000") int queueCapacity,
        @DefaultValue("100000000") long batchLimitInPence) {
}
