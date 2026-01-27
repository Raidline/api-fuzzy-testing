# API Fuzzy Testing Tool

A powerful command-line fuzzy testing tool designed to automatically test API endpoints by generating and sending
randomized or
malformed requests to uncover potential vulnerabilities, edge cases, and unexpected behaviors.

> **Note:** This tool is designed to run exclusively via CLI. GUI support is not planned at this time.
> **Note:** Only supporting localhost for now.

## Overview

This tool allows you to perform comprehensive fuzzy testing on any API by simply providing its schema definition (e.g.,
Swagger/OpenAPI). Point it to your API server, and the tool will systematically test all endpoints with various payloads
to identify failures and unexpected responses.

Why Java? Because i wanted a project where i could learn/investigate the virtualThreads usage in Java. That is why.
Is the Java the best language for CLI? No. But i wanted to do this project and since i want to investigate
virtualThreads in Java, you get CLI app in Java. BIG W!

> **IMPORTANT:** Java HttpClient does not support PATCH HTTP method. I'm not going, for this Java version, to add
> specific
> code to send PATCH requests.
> So, no PATCH requests can be made

> **Note:** If everything goes well, i will probably re-do this in a more nice language for CLI (e.g Go)

### How It Works

1. **Provide an API Schema** – Supply a Swagger/OpenAPI JSON definition file describing your API endpoints
2. **Configure the Server** – Specify the target server location where the API is running
3. **Run Tests** – The tool automatically generates test cases and sends requests to all endpoints
4. **Analyze Results** – By default, tests run continuously until a failure is detected, providing detailed output of
   the failing request

## Features Checklist

### Schema & Endpoint Support

- [x] **Swagger/OpenAPI JSON Support** – Parse and interpret API definitions from Swagger/OpenAPI JSON files
- [x] **Test All Endpoints** – Automatically discover and test all endpoints defined in the schema (default behavior)

### Execution Control

- [x] **Time-Limited Execution** – Define a maximum duration for the test run when no errors occur
- [x] **Run Until Failure** – Default mode that continuously tests until an error is encountered

### Load & Concurrency

- [x] **Custom Number of simultaneous Requests** – Define an upper limit for **all** outgoing requests. Default is 10.
- [x] **Concurrent API Requests** – Support for sending multiple simultaneous requests to the API

### Reporting & Debugging

- [x] **Detailed Failure Output** – Comprehensive logging of failed requests including the full request body, headers,
  and response details

## Getting Started

### Prerequisites

- **Java 25** (with preview features enabled) – Required due to virtual threads and `StructuredTaskScope` usage
- **Maven 3.6+** – For building and running the project

### Building the Project

1. Clone the repository:
   ```bash
   git clone <repository-url>
   cd api-fuzzy-testing
   ```

2. Build the entire project:
   ```bash
   mvn clean compile
   ```

3. (Optional) Start the included test server:
   ```bash
   cd petstore-server
   mvn exec:java
   ```

## Configuration

The tool accepts the following command-line arguments:

| Argument | Description | Required | Default |
|----------|-------------|----------|---------|
| `-f=<filepath>` | Path to the OpenAPI/Swagger JSON or YAML schema file | Yes | - |
| `-s=<server>` | Target server URL (e.g., `http://localhost:8080`) | Yes | - |
| `-t=<seconds>` | Maximum running time in seconds | No | Runs until failure |
| `-lc=<number>` | Maximum number of concurrent requests (throttling) | No | 10 |
| `-d` | Enable debug mode for verbose logging | No | Disabled |

### Schema Files

The tool supports:
- **JSON** format OpenAPI/Swagger specifications
- **YAML** format OpenAPI/Swagger specifications

Example schema files are available in `api-fuzzy/src/main/resources/examples/`.

## Usage

### Using the Run Script

The easiest way to run the tool is using the provided shell script:

```bash
cd api-fuzzy
./run.sh -f=src/main/resources/examples/petstore-example.json -s=http://localhost:8080
```

### Manual Execution

Alternatively, run the tool manually using Maven:

```bash
cd api-fuzzy
mvn clean compile
java -cp "target/classes:target/dependency/*" pt.raidline.api.fuzzy.ApiFuzzyMain -f=<schema-file> -s=<server-url>
```

### Examples

**Basic usage:**
```bash
./run.sh -f=api.json -s=http://localhost:8080
```

**With time limit (60 seconds):**
```bash
./run.sh -f=api.json -s=http://localhost:8080 -t=60
```

**With increased concurrency (20 simultaneous requests):**
```bash
./run.sh -f=api.json -s=http://localhost:8080 -lc=20
```

**With debug mode:**
```bash
./run.sh -f=api.json -s=http://localhost:8080 -d
```

**Complete example with all options:**
```bash
./run.sh -f=src/main/resources/examples/petstore-example.json -s=http://localhost:8080 -t=120 -lc=15 -d
```

### Testing with the Petstore Server

The project includes a sample Petstore server for testing:

1. Start the Petstore server:
   ```bash
   cd petstore-server
   mvn exec:java
   ```

2. In another terminal, run the fuzzy tester:
   ```bash
   cd api-fuzzy
   ./run.sh -f=src/main/resources/examples/petstore-example.json -s=http://localhost:8080
   ```

## Output

The tool provides detailed output including:
- **Request details** – Full HTTP request information (URI, method, headers, body)
- **Response details** – Status code and response body
- **Failure information** – Comprehensive error reports when a request fails

When running with `-d` (debug mode), additional verbose logging is displayed showing the internal processing steps.

