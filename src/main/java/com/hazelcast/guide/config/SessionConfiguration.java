package com.hazelcast.guide.config;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;;
import com.hazelcast.core.HazelcastInstance;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.hazelcast.HazelcastKeyValueAdapter;


@Configuration
public class SessionConfiguration {


    //Hazelcast Client Instance Bean
    @Bean
    public HazelcastInstance hazelcastInstance() {
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.getNetworkConfig().addAddress("127.0.0.1:5701", "127.0.0.1:5702");
        clientConfig.setInstanceName("webcapture");
        clientConfig.setClusterName("dev");
        return HazelcastClient.newHazelcastClient(clientConfig);
    }

    @Bean
    public HazelcastKeyValueAdapter hazelcastKeyValueAdapter() {
        return new HazelcastKeyValueAdapter(hazelcastInstance());
    }


}
