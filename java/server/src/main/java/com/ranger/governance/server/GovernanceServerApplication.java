package com.ranger.governance.server;

import com.ranger.governance.server.config.GovernancePolicyProperties;
import com.ranger.governance.server.config.NotificationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({GovernancePolicyProperties.class, NotificationProperties.class})
public class GovernanceServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(GovernanceServerApplication.class, args);
    }
}
