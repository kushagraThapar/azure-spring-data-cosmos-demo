// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.example.main.repository.db;

import com.azure.spring.data.cosmos.repository.CosmosRepository;
import com.azure.spring.data.cosmos.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends CosmosRepository<User, String> {

    List<User> findByFirstNameAndLastName(String firstName, String lastName);

    List<User> findByLastName(String lastName);

    Optional<User> findByIdAndLastName(String id, String lastName);

    @Query("select * from c where c.lastName = @lastName")
    List<User> findUsersByQueryAnnotation(@Param("lastName") String lastName);
}
