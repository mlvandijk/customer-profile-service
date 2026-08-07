# Customer Profile Service

This is a demo application to illustrate features from project Loom.

## Prerequisites

- JDK 27 EA
- Maven 3.9+

## Branches

- `main` uses platform threads, `CompletableFuture`, `ThreadLocal`.
- `modern` uses virtual threads([JEP 444](https://openjdk.org/jeps/444)), scoped values ([JEP 506](https://openjdk.org/jeps/506)), and structured concurrency ([JEP 533 preview](https://openjdk.org/jeps/533)).

## Running the demo

Start `services`:

```bash
./mvnw spring-boot:run -pl services
```

Start `profile-service` in a second terminal:

```bash
./mvnw spring-boot:run -pl profile-service
```

Both applications must be running simultaneously.

## Sending requests

The `.http` files in the repository root contain ready-made requests for the
[HTTP Client](https://www.jetbrains.com/help/idea/http-client-in-product-code-editor.html)
in [IntelliJ IDEA](https://lp.jetbrains.com/intellij-idea-promo/). 
Open a file and run all requests in the file by clicking the ▶▶ **Run All Requests in File** button at the top of the file, or an individual request by clicking the ▶ icon in the gutter.

`services.http` targets the downstream stubs in `services` on port 8081.
Run all requests to make sure the downstream services are running correctly.

`requests.http` targets `profile-service` on port 8080.
Run the relevant request to see how the profile service behaves.

## Running tests

Run individual test classes or tests from IntelliJ IDEA by clicking the ▶ icon in the gutter, or the relevant **Run Configuration**.

Run all unit tests from the repository root:

```bash
./mvnw test
```

## Building the project

Build the project from the repository root:

```bash
./mvnw verify
```
This will also run all unit and integration tests.