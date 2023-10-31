// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.example.main;

import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.util.Context;
import com.azure.cosmos.CosmosAsyncClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.CosmosDiagnosticsContext;
import com.azure.cosmos.CosmosDiagnosticsHandler;
import com.azure.cosmos.CosmosDiagnosticsThresholds;
import com.azure.cosmos.DirectConnectionConfig;
import com.azure.cosmos.models.CosmosClientTelemetryConfig;
import com.azure.spring.data.cosmos.config.AbstractCosmosConfiguration;
import com.azure.spring.data.cosmos.config.CosmosConfig;
import com.azure.spring.data.cosmos.core.CosmosTemplate;
import com.azure.spring.data.cosmos.core.ReactiveCosmosTemplate;
import com.azure.spring.data.cosmos.core.ResponseDiagnostics;
import com.azure.spring.data.cosmos.core.ResponseDiagnosticsProcessor;
import com.azure.spring.data.cosmos.core.convert.MappingCosmosConverter;
import com.azure.spring.data.cosmos.core.mapping.EnableCosmosAuditing;
import com.azure.spring.data.cosmos.repository.config.EnableCosmosRepositories;
import com.azure.spring.data.cosmos.repository.config.EnableReactiveCosmosRepositories;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.math.ec.ECCurve;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.lang.Nullable;

import java.time.Duration;
import java.util.function.BiPredicate;

@Configuration
@EnableCosmosRepositories(basePackages = "com.example.main.repository.db1", cosmosTemplateRef = "primaryDatabaseTemplate")
@EnableReactiveCosmosRepositories(basePackages = "com.example.main.repository.db1", reactiveCosmosTemplateRef = "primaryDatabaseReactiveTemplate")
@PropertySource("classpath:application.properties")
@EnableCosmosAuditing
@Slf4j
@Data
@RefreshScope
public class PrimaryDataSourceConfiguration extends AbstractCosmosConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(PrimaryDataSourceConfiguration.class);

    private AzureKeyCredential azureKeyCredential;

    @Bean
    @ConfigurationProperties(prefix = "azure.cosmos.primary")
    public CosmosProperties primary() {
        return new CosmosProperties();
    }

    @Bean
    public CosmosClientBuilder primaryCosmosClientBuilder(@Qualifier("primary") CosmosProperties cosmosProperties) {
        DirectConnectionConfig directConnectionConfig = DirectConnectionConfig.getDefaultConfig();
        this.azureKeyCredential = new AzureKeyCredential(cosmosProperties.getKey());
        return new CosmosClientBuilder()
            .endpoint(cosmosProperties.getUri())
            .credential(azureKeyCredential)
            .clientTelemetryConfig(getCosmosTelemetryConfig())
            .directMode(directConnectionConfig);
    }

    private CosmosClientTelemetryConfig getCosmosTelemetryConfig() {
        CosmosClientTelemetryConfig cosmosClientTelemetryConfig = new CosmosClientTelemetryConfig();
        cosmosClientTelemetryConfig.enableTransportLevelTracing();
        cosmosClientTelemetryConfig.diagnosticsHandler((diagnosticsContext, traceContext) -> logger.info("handleDiagnostics {}",
            diagnosticsContext.getDiagnostics()));
        cosmosClientTelemetryConfig.diagnosticsThresholds(getDiagnosticThresholds());
        return cosmosClientTelemetryConfig;
    }

    private CosmosDiagnosticsThresholds getDiagnosticThresholds() {
        CosmosDiagnosticsThresholds cosmosDiagnosticsThresholds = new CosmosDiagnosticsThresholds();
        //cosmosDiagnosticsThresholds.setRequestChargeThreshold(100.0f);
        cosmosDiagnosticsThresholds.setPointOperationLatencyThreshold(Duration.ofSeconds(1));
        cosmosDiagnosticsThresholds.setNonPointOperationLatencyThreshold(Duration.ofSeconds(3));
//        cosmosDiagnosticsThresholds.setFailureHandler(new BiPredicate<Integer, Integer>() {
//            @Override
//            public boolean test(Integer statusCode, Integer subStatusCode) {
//                return statusCode >= 400;
//            }
//        });
        return cosmosDiagnosticsThresholds;
    }

    @Bean
    public CosmosConfig cosmosConfig(@Qualifier("primary") CosmosProperties cosmosProperties) {
        return CosmosConfig.builder()
//                           .responseDiagnosticsProcessor(new ResponseDiagnosticsProcessorImplementation())
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

//    private static class ResponseDiagnosticsProcessorImplementation implements ResponseDiagnosticsProcessor {
//
//        @Override
//        public void processResponseDiagnostics(@Nullable ResponseDiagnostics responseDiagnostics) {
//            logger.info("Response Diagnostics {}", responseDiagnostics);
//        }
//    }

    public AzureKeyCredential getAzureKeyCredential() {
        return azureKeyCredential;
    }
}
