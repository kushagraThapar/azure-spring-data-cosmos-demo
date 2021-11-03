// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.example.main;

import com.azure.cosmos.CosmosAsyncClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.DirectConnectionConfig;
import com.azure.spring.data.cosmos.config.AbstractCosmosConfiguration;
import com.azure.spring.data.cosmos.config.CosmosConfig;
import com.azure.spring.data.cosmos.core.CosmosTemplate;
import com.azure.spring.data.cosmos.core.ReactiveCosmosTemplate;
import com.azure.spring.data.cosmos.core.ResponseDiagnostics;
import com.azure.spring.data.cosmos.core.ResponseDiagnosticsProcessor;
import com.azure.spring.data.cosmos.core.convert.MappingCosmosConverter;
import com.azure.spring.data.cosmos.repository.config.EnableCosmosRepositories;
import com.azure.spring.data.cosmos.repository.config.EnableReactiveCosmosRepositories;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.lang.Nullable;

@Configuration
@EnableCosmosRepositories(basePackages = "com.example.main.repository.db1", cosmosTemplateRef = "primaryDatabaseTemplate")
@EnableReactiveCosmosRepositories(basePackages = "com.example.main.repository.db1", reactiveCosmosTemplateRef = "primaryDatabaseReactiveTemplate")
@PropertySource("classpath:application.properties")
public class PrimaryDataSourceConfiguration extends AbstractCosmosConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(PrimaryDataSourceConfiguration.class);

    @Bean
    @ConfigurationProperties(prefix = "azure.cosmos.primary")
    public CosmosProperties primary() {
        return new CosmosProperties();
    }

    @Bean
    public CosmosClientBuilder primaryCosmosClientBuilder(@Qualifier("primary") CosmosProperties cosmosProperties) {
        DirectConnectionConfig directConnectionConfig = DirectConnectionConfig.getDefaultConfig();
        return new CosmosClientBuilder()
            .endpoint(cosmosProperties.getUri())
            .key(cosmosProperties.getKey())
            .directMode(directConnectionConfig);
    }

    @Bean
    public CosmosConfig cosmosConfig(@Qualifier("primary") CosmosProperties cosmosProperties) {
        return CosmosConfig.builder()
                           .responseDiagnosticsProcessor(new ResponseDiagnosticsProcessorImplementation())
                           .enableQueryMetrics(cosmosProperties.isQueryMetricsEnabled())
                           .build();
    }

    @Bean
    public ReactiveCosmosTemplate primaryDatabaseReactiveTemplate(CosmosAsyncClient cosmosAsyncClient,
                                                          CosmosConfig cosmosConfig,
                                                          MappingCosmosConverter mappingCosmosConverter) {
        return new ReactiveCosmosTemplate(cosmosAsyncClient, getDatabaseName(), cosmosConfig, mappingCosmosConverter);
    }

    @Bean
    public CosmosTemplate primaryDatabaseTemplate(CosmosAsyncClient cosmosAsyncClient,
                                                  CosmosConfig cosmosConfig,
                                                  MappingCosmosConverter mappingCosmosConverter) {
        return new CosmosTemplate(cosmosAsyncClient, getDatabaseName(), cosmosConfig, mappingCosmosConverter);
    }

    protected String getDatabaseName() {
        return "testdb";
    }

    private static class ResponseDiagnosticsProcessorImplementation implements ResponseDiagnosticsProcessor {

        @Override
        public void processResponseDiagnostics(@Nullable ResponseDiagnostics responseDiagnostics) {
            logger.info("Response Diagnostics {}", responseDiagnostics);
        }
    }
}