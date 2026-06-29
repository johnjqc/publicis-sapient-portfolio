#!/bin/sh

echo "Creating topics..."

KAFKA_BROKER="kafka:9092"

kafka-topics --create --if-not-exists \
  --bootstrap-server "$KAFKA_BROKER" \
  --topic market.price.updated \
  --partitions 1 \
  --replication-factor 1 \
  --config retention.ms=3600000 \
  --config compression.type=snappy

echo "✓ Created topic: market.price.updated (1 partitions, 1h retention)"

# Pricing Engine → Portfolio Service
kafka-topics --create \
  --bootstrap-server "$KAFKA_BROKER" \
  --topic price.snapshot.updated \
  --partitions 1 \
  --replication-factor 1 \
  --if-not-exists \
  --config retention.ms=3600000 \
  --config compression.type=snappy

echo "✓ Created topic: price.snapshot.updated (1 partitions, 1h retention)"


echo ""
echo "All topics created successfully!"
echo ""
echo "Topics in use:"
kafka-topics --list --bootstrap-server "$KAFKA_BROKER"