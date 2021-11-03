// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.example.main.repository.db1;

import com.azure.spring.data.cosmos.repository.CosmosRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository1 extends CosmosRepository<User1, String> {

    Iterable<User1> findByFirstName(String firstName);

    User1 findByIdAndLastName(String id, String lastName);
}
