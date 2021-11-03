// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.example.main.repository.db1;

import com.azure.spring.data.cosmos.repository.ReactiveCosmosRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface ReactiveUserRepository1 extends ReactiveCosmosRepository<User1, String> {

    Flux<User1> findByFirstName(String firstName);
}
