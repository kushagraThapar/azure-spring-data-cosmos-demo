// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.example.main;

import com.azure.cosmos.CosmosAsyncClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.DirectConnectionConfig;
import com.azure.spring.data.cosmos.CosmosFactory;
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
@EnableCosmosRepositories(basePackages = "com.example.main.repository.db2", cosmosTemplateRef = "secondaryDatabaseTemplate")
@EnableReactiveCosmosRepositories(basePackages = "com.example.main.repository.db2", reactiveCosmosTemplateRef = "secondaryDatabaseReactiveTemplate")
@PropertySource("classpath:application.properties")
public class SecondaryDataSourceConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(SecondaryDataSourceConfiguration.class);

    @Bean
    @ConfigurationProperties(prefix = "azure.cosmos.secondary")
    public CosmosProperties secondary() {
        return new CosmosProperties();
    }

    @Bean("secondaryCosmosAsyncClient")
    public CosmosAsyncClient secondaryCosmosAsyncClient(@Qualifier("secondary") CosmosProperties cosmosProperties) {
        DirectConnectionConfig directConnectionConfig = DirectConnectionConfig.getDefaultConfig();
        return CosmosFactory.createCosmosAsyncClient(new CosmosClientBuilder()
            .endpoint(cosmosProperties.getUri())
            .key(cosmosProperties.getKey())
            .directMode(directConnectionConfig));
    }

    @Bean("secondaryCosmosConfig")
    public CosmosConfig secondaryCosmosConfig(@Qualifier("secondary") CosmosProperties cosmosProperties) {
        return CosmosConfig.builder()
                           .responseDiagnosticsProcessor(new ResponseDiagnosticsProcessorImplementation())
                           .enableQueryMetrics(cosmosProperties.isQueryMetricsEnabled())
                           .build();
    }

    @Bean
    public ReactiveCosmosTemplate secondaryDatabaseReactiveTemplate(@Qualifier("secondaryCosmosAsyncClient") CosmosAsyncClient cosmosAsyncClient,
                                                          @Qualifier("secondaryCosmosConfig") CosmosConfig cosmosConfig,
                                                          MappingCosmosConverter mappingCosmosConverter) {
        return new ReactiveCosmosTemplate(cosmosAsyncClient, getDatabaseName(), cosmosConfig, mappingCosmosConverter);
    }

    @Bean
    public CosmosTemplate secondaryDatabaseTemplate(@Qualifier("secondaryCosmosAsyncClient") CosmosAsyncClient cosmosAsyncClient,
                                                    @Qualifier("secondaryCosmosConfig") CosmosConfig cosmosConfig,
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
