// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.example.main.repository.db2;

import com.azure.spring.data.cosmos.repository.CosmosRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository2 extends CosmosRepository<User2, String> {

    Iterable<User2> findByFirstName(String firstName);

    User2 findByIdAndLastName(String id, String lastName);
}
