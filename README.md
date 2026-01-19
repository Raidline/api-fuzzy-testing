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
- [ ] **Single Endpoint Testing** – Option to target and test a specific endpoint in isolation

### Execution Control

- [ ] **Time-Limited Execution** – Define a maximum duration for the test run when no errors occur
- [ ] **Custom Stop Conditions** – Configure specific HTTP status codes (e.g., 502, 500) or response patterns that
  should halt execution
- [ ] **Run Until Failure** – Default mode that continuously tests until an error is encountered

### Load & Concurrency

- [ ] **Custom Number of simultaneous Requests** – Define an upper limit for outgoing requests. Default is 10.
- [x] **Concurrent API Requests** – Support for sending multiple simultaneous requests to the API
- [ ] **Concurrent Requests** – Support for sending multiple simultaneous requests to the same endpoint
- [ ] **Exponential User Growth** – Define an initial number of virtual users and configure automatic exponential
  scaling to simulate increasing load

### Reporting & Debugging

- [ ] **Detailed Failure Output** – Comprehensive logging of failed requests including the full request body, headers,
  and response details
- [ ] **Failure Replay** – Ability to re-run a specific test case that caused a failure for debugging and verification
  purposes

## Getting Started

*Coming soon in the theaters*

## Configuration

*Coming soon in the theaters*

## Usage

*Coming soon in the theaters*

