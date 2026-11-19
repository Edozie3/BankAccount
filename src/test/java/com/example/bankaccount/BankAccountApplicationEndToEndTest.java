package com.example.bankaccount;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.example.bankaccount.api.BalanceResponse;
import com.example.bankaccount.audit.AuditPublisher;
import com.example.bankaccount.audit.Batch;
import com.example.bankaccount.audit.Submission;
import com.example.bankaccount.config.AuditProperties;
import java.math.BigDecimal;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;

/**
 * Runs the application on a random port: producers → balance tracker → audit queue → audit worker →
 * batching → publisher, with the balance served over HTTP.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "audit.submission-size=50")
class BankAccountApplicationEndToEndTest {

    @LocalServerPort
    private int port;

    @Autowired
    private AuditProperties auditProperties;

    @MockitoBean
    private AuditPublisher auditPublisher;

    @Test
    void givenApplicationRunning_whenWaiting_thenSubmissionsOfTheConfiguredSizeArePublished() {
        // given: the application context has started the producers and the audit worker

        // when
        ArgumentCaptor<Submission> published = ArgumentCaptor.forClass(Submission.class);
        verify(auditPublisher, timeout(10_000).atLeastOnce()).publish(published.capture());

        // then
        Submission submission = published.getValue();
        assertThat(submission.batches().stream().mapToInt(Batch::countOfTransactions).sum())
                .isEqualTo(auditProperties.submissionSize());
        assertThat(submission.batches()).allSatisfy(batch ->
                assertThat(batch.totalValueInPence()).isLessThanOrEqualTo(auditProperties.batchLimitInPence()));
    }

    @Test
    void givenApplicationRunning_whenGettingBalanceOverHttp_thenReturnsGbpBalance() {
        // given
        RestClient client = RestClient.create("http://localhost:" + port);

        // when / then: producers are running, so the balance soon moves away from zero
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            BalanceResponse response = client.get().uri("/api/v1/account/balance")
                    .retrieve()
                    .body(BalanceResponse.class);
            assertThat(response).isNotNull();
            assertThat(response.currency()).isEqualTo("GBP");
            assertThat(response.balance()).isNotEqualByComparingTo(BigDecimal.ZERO);
        });
    }
}
