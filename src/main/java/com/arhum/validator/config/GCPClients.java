package com.arhum.validator.config;

import com.google.cloud.compute.v1.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class GCPClients {
    private static final Logger logger = LoggerFactory.getLogger(GCPClients.class);

    @Bean(destroyMethod = "close")
    public FirewallsClient firewallsClient() throws IOException {
        logger.info("INIT FIREWALL CLIENT");
        return FirewallsClient.create();
    }

    @Bean(destroyMethod = "close")
    public InstancesClient instancesClient() throws IOException {
        logger.info("INIT INSTANCE CLIENT");
        return InstancesClient.create();
    }

    @Bean(destroyMethod = "close")
    public MachineTypesClient machineTypesClient() throws IOException {
        logger.info("INIT MACHINETYPE CLIENT");
        return MachineTypesClient.create();
    }
}
