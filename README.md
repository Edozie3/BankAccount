# Bank Account

A Spring Boot service that processes a continuous stream of transactions on a bank account. It tracks the running balance, exposes it over a REST API, and sends every transaction to an audit system in submissions arranged in batches.

## Design

Two producer threads generate random transactions, one for credits and one for debits. Amounts range from £200 to £500,000 with credits being positive and debits negative.

Each transaction goes to the balance tracker, which updates the balance atomically and puts the transaction on a bounded audit queue. A single audit worker takes transactions off the queue. Once it holds a full submission (1,000 transactions by default) it splits them into batches whose absolute value is at most £1,000,000 and prints the submission to the console.

```
credit producer ─┐
                 ├─► BalanceTracker ──► audit queue ──► AuditWorker ──► BatchingStrategy ──► AuditPublisher
debit producer  ─┘         │                                                                  (console)
                           └──► GET /api/v1/account/balance
```

| Package | Responsibility                                                              |
|---|-----------------------------------------------------------------------------|
| `transaction` | `Transaction` data record                                                   |
| `producer` | Random transaction generation and the fixed-rate credit and debit producers |
| `service` | `BankAccountService` and its implementation `BalanceTracker`                |
| `audit` | Audit worker, batching strategy and console publisher                       |
| `api` | REST controller for retrieving the balance                                  |
| `config` | Bean and property configuration                                             |

### Batching

Submissions are grouped using a Best-Fit Decreasing strategy. Transactions are sorted by absolute amount in descending order and placed into the existing batch with the least remaining capacity that can accommodate them. A new batch is created only when no existing batch has sufficient space. This approach minimises wasted capacity and keeps the number of batches as low as possible.

## Requirements

- JDK 21 or later
- Maven 3.9 or later

## Build and test

```
mvn clean package
```

## Run

### Default profile

Submissions of 1,000 transactions.

```
mvn spring-boot:run
```

or

```
java -jar target/bank-account-0.0.1-SNAPSHOT.jar
```

### Scale profile

Submissions of 100,000 transactions (`src/main/resources/application-scale.yml`).

```
mvn spring-boot:run -Dspring-boot.run.profiles=scale
```

or

```
java -jar target/bank-account-0.0.1-SNAPSHOT.jar --spring.profiles.active=scale
```

(With this profile, the first submission is printed after approximately 33 minutes, as generating 100,000 transactions at a rate of 50 per second a while.)

## Account Balance API

```
curl http://localhost:8080/api/v1/account/balance
```

Expected output :

```json
{"balance":12345.67,"currency":"GBP"}
```

## Benchmarks

`BatchingBenchmark` uses JMH to measure the average time it takes to batch submissions at sizes ranging from 1,000 to 1,000,000 transactions. 

Run the benchmark using:

```
mvn test-compile exec:exec
```

The results below were recorded using JDK 21:

```
Transactions per submission  Average time to batch (ms)  Margin of error (ms)  Measurements  Increase on previous size
                      1,000                        0.74               ± 0.08            30
                     10,000                        9.14               ± 0.65            30                      12.4x
                    100,000                      131.27               ± 4.28            30                      14.4x
                  1,000,000                    1,938.25              ± 62.53            30                      14.8x
```

As submission size increases tenfold, batching takes roughly 12–15 times longer. This is consistent with the expected performance of the sorting and lookup operations in the batching strategy( O(n log n) ).