// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.example.main;

import com.azure.cosmos.models.PartitionKey;
import com.example.main.repository.db.ReactiveUserRepository;
import com.example.main.repository.db.User;
import com.example.main.repository.db.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.DependsOn;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@SpringBootApplication
@DependsOn("expressionResolver")
public class SampleApplication implements CommandLineRunner {

    private final Logger logger = LoggerFactory.getLogger(SampleApplication.class);

    private final UserRepository userRepository;

    private final ReactiveUserRepository reactiveUserRepository;

    public SampleApplication(UserRepository userRepository, ReactiveUserRepository reactiveUserRepository) {
        this.userRepository = userRepository;
        this.reactiveUserRepository = reactiveUserRepository;
    }

    public static void main(String[] args) {
        SpringApplication.run(SampleApplication.class, args);
    }

    public void run(String... var1) {

        final User testUser1 = new User("testId1", "testFirstName1", "testLastName1");
        final User testUser2 = new User("testId2", "testFirstName2", "testLastName2");
        final User testUser3 = new User("testId3", "testFirstName3", "testLastName2");
        final User testUser4 = new User("testId4", "testFirstName4", "testLastName3");
        final User testUser5 = new User("testId5", "testFirstName5", "testLastName3");
        final User testUser6 = new User("testId6", "testFirstName6", "testLastName2");
        final User testUser7 = new User("testId7", "testFirstName7", "testLastName1");
        final User testUser8 = new User("testId8", "testFirstName8", "testLastName1");

        final List<User> users = new ArrayList<>();
        users.add(testUser1);
        users.add(testUser2);
        users.add(testUser3);
        users.add(testUser4);
        users.add(testUser5);
        users.add(testUser6);
        users.add(testUser7);
        users.add(testUser8);

        logger.info("Using sync repository");

        //        userRepository.deleteAll();
        //
        //        logger.info("Saving all users");
        //        userRepository.saveAll(users);

        logger.info("Finding user by id and partition key - point read call");
        final Optional<User> result1 = userRepository.findById(testUser1.getId(),
            new PartitionKey(testUser1.getLastName()));
        logger.info("Found user : {}", result1.isPresent());

        logger.info("Finding user by id and partition key using repository API - query operation");
        Optional<User> byIdAndLastName = userRepository.findByIdAndLastName(testUser2.getId(), testUser2.getLastName());
        logger.info("Found user : {}", byIdAndLastName.isPresent());


        logger.info("Finding user by testLastName2");
        List<User> usersByLastName = userRepository.findByLastName("testLastName2");
        logger.info("Found {} users by testLastName2", usersByLastName.size());

        logger.info("Finding all users by partition key");
        Iterable<User> findAllUsers = userRepository.findAll(new PartitionKey("testLastName1"));
        findAllUsers.forEach(user -> {
            logger.info("Found user : {}", user);
        });

        logger.info("Finding all users by partition key again to check query plan cache");
        findAllUsers = userRepository.findAll(new PartitionKey("testLastName2"));
        findAllUsers.forEach(user -> {
            logger.info("Found user : {}", user);
        });

        logger.info("Finding users by query annotation");
        List<User> findUsersByQueryAnnotation = userRepository.findUsersByQueryAnnotation("testLastName3");
        logger.info("Found {} users by query annotation", findUsersByQueryAnnotation.size());

        logger.info("Getting exception diagnostics");
        Optional<User> something = userRepository.findById("wrong-id", new PartitionKey("wrong-key"));
        logger.info("Found user : {}", something.isPresent());

        logger.info("Using reactive repository");

        Flux<User> users1 = reactiveUserRepository.findByFirstName("testFirstName");
        users1.map(u -> {
            logger.info("user is : {}", u);
            return u;
        }).subscribe();
    }
}
