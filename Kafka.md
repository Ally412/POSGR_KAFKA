# Kafka Topics

## Core Concepts
- Brokers and clusters
- Topics and partitions
- Replicas and leadership
- Retention policies (time-based, size-based)
- Log-based architecture

## Producers
- Sending messages to topics
- Serialization and deserialization
- Partitioning strategy
- Acknowledgments (acks: 0, 1, all)
- Batching and compression
- Error handling and retries
- Idempotent producers

## Consumers
- Consumer groups
- Offsets and offset management (auto-commit, manual)
- Rebalancing
- Lag monitoring
- Polling and message processing
- Consumer configurations

## Message Format
- Keys and values
- Headers
- Timestamps
- Serialization formats (JSON, Avro, Protobuf)
- Schema compatibility

## Event-Driven Architecture
- Event sourcing basics
- Domain events
- Event publishing and subscribing
- Saga pattern (distributed transactions)

## Error Handling
- Dead Letter Queues (DLQ)
- Retry policies
- Exception handling in consumers
- Monitoring and alerting

## Spring Kafka Integration
- @KafkaListener annotation
- KafkaTemplate for sending
- Message converters
- Error handling strategies
- Transactions in Kafka

## Docker & Local Setup
- Docker Compose for Kafka + Zookeeper
- Kafka CLI tools (topics, console producers/consumers)
- Monitoring tools (Kafdrop, Confluent Control Center)

## Testing
- Embedded Kafka for tests
- Testcontainers with Kafka
- Mock producers and consumers
