package main

import "gopkg.in/Shopify/sarama.v1"
import "log"
import "os"
import "os/signal"
import "fmt"
import "strings"

func main() {
	topic := os.Getenv("KAFKA_TOPIC")
	hosts := strings.Split(os.Getenv("KAFKA_HOSTS"), ",")
	log.Printf("(%s) [%s]\n", hosts, topic)
	config := sarama.NewConfig()
	consumer, err := sarama.NewConsumer(hosts, config)
	if err != nil {
		panic(err)
	}

	defer func() {
		if err := consumer.Close(); err != nil {
			log.Fatalln(err)
		}
	}()

	partitions, err := consumer.Partitions(topic)

	if err != nil {
		panic(err)
	}

	for _, element := range partitions {
		go consumePartition(consumer, topic, element)
	}
	signals := make(chan os.Signal, 1)
	signal.Notify(signals, os.Interrupt)
	for true {
		select {
		case <-signals:
			os.Exit(1)
		}
	}
}

func consumePartition(consumer sarama.Consumer, topic string, partition int32) {
    log.Printf("Starting Consumer for %v, partition %d\n", topic, partition)
	partitionConsumer, err := consumer.ConsumePartition(topic, partition, sarama.OffsetNewest)
	if err != nil {
		panic(err)
	}

	defer func() {
		if err := partitionConsumer.Close(); err != nil {
			log.Fatalln(err)
		}
	}()

	// Trap SIGINT to trigger a shutdown.
	signals := make(chan os.Signal, 1)
	signal.Notify(signals, os.Interrupt)

	consumed := 0
	ConsumerLoop:
	for {
		select {
		case msg := <-partitionConsumer.Messages():
			fmt.Printf("%v", string(msg.Value)) 
			consumed++
		case <-signals:
			break ConsumerLoop
		}
	}

}