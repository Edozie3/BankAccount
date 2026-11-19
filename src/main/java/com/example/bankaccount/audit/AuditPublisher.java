package com.example.bankaccount.audit;

/**
 * Sends submissions to the downstream audit system.
 */
public interface AuditPublisher {

    void publish(Submission submission);
}
