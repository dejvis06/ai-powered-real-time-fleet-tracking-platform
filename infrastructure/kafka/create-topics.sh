#!/bin/bash
# Creates required Kafka topics with 4 partitions (vehicle-location partitioned by deliveryId).

KAFKA_BIN=${KAFKA_HOME:-/opt/kafka}/bin
BOOTSTRAP=${KAFKA_BOOTSTRAP:-localhost:9092}

create_topic() {
  local topic=$1
  local partitions=$2
  local replicas=$3

  $KAFKA_BIN/kafka-topics.sh \
    --bootstrap-server "$BOOTSTRAP" \
    --create \
    --if-not-exists \
    --topic "$topic" \
    --partitions "$partitions" \
    --replication-factor "$replicas"
}

echo "Creating Kafka topics..."

create_topic vehicle-location    4 1
create_topic eta-updated         4 1
create_topic delivery-started    1 1
create_topic delivery-completed  1 1
create_topic delivery-failed     1 1

echo "Topics created."

$KAFKA_BIN/kafka-topics.sh --bootstrap-server "$BOOTSTRAP" --list
