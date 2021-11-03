// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.example.main.repository.db2;

import com.azure.spring.data.cosmos.repository.ReactiveCosmosRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface ReactiveUserRepository2 extends ReactiveCosmosRepository<User2, String> {

    Flux<User2> findByFirstName(String firstName);
}
