// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.example.main;

import com.azure.cosmos.ChangeFeedProcessor;
import com.azure.cosmos.ChangeFeedProcessorBuilder;
import com.azure.cosmos.CosmosAsyncClient;
import com.azure.cosmos.CosmosAsyncContainer;
import com.azure.cosmos.CosmosAsyncDatabase;
import com.azure.cosmos.models.ChangeFeedProcessorItem;
import com.azure.cosmos.models.ChangeFeedProcessorOptions;
import com.azure.cosmos.models.PartitionKey;
import com.azure.spring.data.cosmos.core.CosmosTemplate;
import com.azure.spring.data.cosmos.core.ReactiveCosmosTemplate;
import com.example.main.repository.db1.ReactiveUserRepository1;
import com.example.main.repository.db1.User1;
import com.example.main.repository.db1.UserRepository1;
import com.example.main.repository.db2.ReactiveUserRepository2;
import com.example.main.repository.db2.User2;
import com.example.main.repository.db2.UserRepository2;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

@SpringBootApplication
@DependsOn("expressionResolver")
public class SampleApplication implements CommandLineRunner {

    private final Logger logger = LoggerFactory.getLogger(SampleApplication.class);

    @Autowired
    private UserRepository1 userRepository1;

    @Autowired
    private ReactiveUserRepository1 reactiveUserRepository1;

    @Autowired
    private UserRepository2 userRepository2;

    @Autowired
    private ReactiveUserRepository2 reactiveUserRepository2;

    @Autowired
    private PrimaryDataSourceConfiguration primaryDataSourceConfiguration;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private CosmosTemplate cosmosTemplate;

    @Autowired
    private ReactiveCosmosTemplate reactiveCosmosTemplate;

    public static void main(String[] args) {
        SpringApplication.run(SampleApplication.class, args);
    }

    public void run(String... var1) {

        ChangeFeedProcessorOptions changeFeedProcessorOptions = new ChangeFeedProcessorOptions();
        changeFeedProcessorOptions.setStartFromBeginning(true);
        changeFeedProcessorOptions.setLeasePrefix("SOME_PREFIX");
        changeFeedProcessorOptions.setFeedPollDelay(Duration.ofMillis(0L).plusSeconds(5));

        CosmosAsyncClient cac = primaryDataSourceConfiguration
                .primaryCosmosClientBuilder(new CosmosProperties()).buildAsyncClient();
        CosmosAsyncContainer cosmosMonitoredContainer = cac.getDatabase(primaryDataSourceConfiguration.getDatabaseName())
                .getContainer(reactiveCosmosTemplate.getContainerName(User1.class));
        cac.getDatabase(primaryDataSourceConfiguration.getDatabaseName())
                .createContainerIfNotExists("targetContainer", "/id").block();
        CosmosAsyncContainer cosmosTargetContainer = cac.getDatabase(primaryDataSourceConfiguration.getDatabaseName())
                .getContainer("targetContainer");
        cac.getDatabase(primaryDataSourceConfiguration.getDatabaseName())
                .createContainerIfNotExists("leaseContainer", "/id").block();
        CosmosAsyncContainer leaseTargetContainer = cac.getDatabase(primaryDataSourceConfiguration.getDatabaseName())
                .getContainer("leaseContainer");
        try {
            ChangeFeedProcessor cfp = new ChangeFeedProcessorBuilder().options(changeFeedProcessorOptions)
                .hostName(InetAddress.getLocalHost().getHostName())
                .feedContainer(cosmosMonitoredContainer).leaseContainer(leaseTargetContainer)
                .handleChanges((List<JsonNode> docs) -> {

                    docs.forEach(doc -> {
                        cosmosTargetContainer.upsertItem(new User1(doc.get("id").toString(),
                                doc.get("firstName").toString(), doc.get("lastName").toString())).block();
                    });

                }).buildChangeFeedProcessor();

            cfp.start().subscribeOn(Schedulers.boundedElastic())
                    .timeout(Duration.ofMillis(2 * 2500))
                    .subscribe();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }

        final User1 testUser11 = new User1("testId1", "testFirstNameA", "testLastName1");
        final User1 testUser12 = new User1("testId2", "testFirstNameA", "testLastName2");

        final User2 testUser21 = new User2("testId3", "testFirstNameB", "testLastName1");
        final User2 testUser22 = new User2("testId4", "testFirstNameB", "testLastName2");

        logger.info("Using sync repository");

        userRepository1.deleteAll();
        userRepository2.deleteAll();

        logger.info("Saving user : {}", testUser11);
        userRepository1.save(testUser11);
        logger.info("Saving user : {}", testUser21);
        userRepository2.save(testUser21);

        logger.info("Saving user : {}", testUser12);
        userRepository1.save(testUser12);
        logger.info("Saving user : {}", testUser22);
        userRepository2.save(testUser22);

        // to find by Id, please specify partition key value if collection is partitioned
//        final User result = userRepository.findByIdAndLastName(testUser1.getId(), testUser1.getLastName());
        final Optional<User1> result1 = userRepository1.findById(testUser11.getId(), new PartitionKey(testUser11.getLastName()));
        logger.info("Found user : {}", result1.isPresent());

        Iterator<User1> usersIterator1 = userRepository1.findByFirstName("testFirstName").iterator();

        logger.info("Users by firstName : testFirstName");
        while (usersIterator1.hasNext()) {
            logger.info("user is : {}", usersIterator1.next());
        }

        final Optional<User2> result2 = userRepository2.findById(testUser21.getId(), new PartitionKey(testUser21.getLastName()));
        logger.info("Found user : {}", result2.isPresent());

        Iterator<User2> usersIterator2 = userRepository2.findByFirstName("testFirstName").iterator();

        logger.info("Users by firstName : testFirstName");
        while (usersIterator2.hasNext()) {
            logger.info("user is : {}", usersIterator2.next());
        }

        logger.info("Updating the key now");
        this.primaryDataSourceConfiguration.getAzureKeyCredential().update("Invalid key");

        logger.info("Using reactive repository");

        Flux<User1> users1 = reactiveUserRepository1.findByFirstName("testFirstName");
        users1.map(u -> {
            logger.info("user is : {}", u);
            return u;
        }).subscribe();

        logger.info("Using reactive repository");

        Flux<User2> users2 = reactiveUserRepository2.findByFirstName("testFirstName");
        users2.map(u -> {
            logger.info("user is : {}", u);
            return u;
        }).subscribe();
    }
}
