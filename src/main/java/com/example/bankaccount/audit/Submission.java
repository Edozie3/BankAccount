package com.example.bankaccount.audit;

import java.util.List;

/**
 * A submission to the audit system consisting of a fixed number of transactions split into one or more batches.
 *
 * @param batches the batches making up this submission
 */
public record Submission(List<Batch> batches) {

    public Submission {
        batches = List.copyOf(batches);
    }
}
