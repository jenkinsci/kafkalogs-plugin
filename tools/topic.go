/*
 * The MIT License
 *
 * Copyright (c) 2018 OC Tanner
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
 
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
			fmt.Printf("%v\n", string(msg.Value)) 
			consumed++
		case <-signals:
			break ConsumerLoop
		}
	}

}