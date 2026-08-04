package com.fleettracking.ingestion.infrastructure.mqtt;

import com.fleettracking.common.topics.MqttTopics;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.MessageChannel;

@EnableIntegration
@Configuration
public class MqttIngestionConfig {

    @Value("${mqtt.broker-url}")
    private String brokerUrl;

    @Value("${mqtt.client-id:mqtt-ingestion-service}")
    private String clientId;

    @Value("${mqtt.username:#{null}}")
    private String username;

    @Value("${mqtt.password:#{null}}")
    private String password;

    @Bean
    public MqttPahoClientFactory mqttClientFactory() {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        MqttConnectOptions options = new MqttConnectOptions();
        options.setServerURIs(new String[]{brokerUrl});
        options.setCleanSession(true);
        options.setAutomaticReconnect(true);
        if (username != null) options.setUserName(username);
        if (password != null) options.setPassword(password.toCharArray());
        factory.setConnectionOptions(options);
        return factory;
    }

    @Bean
    public MessageChannel mqttInputChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageProducer mqttInbound(MqttPahoClientFactory mqttClientFactory,
                                       MessageChannel mqttInputChannel) {
        MqttPahoMessageDrivenChannelAdapter adapter = new MqttPahoMessageDrivenChannelAdapter(
                clientId,
                mqttClientFactory,
                MqttTopics.VEHICLE_LOCATION
        );
        adapter.setCompletionTimeout(5000);
        DefaultPahoMessageConverter converter = new DefaultPahoMessageConverter();
        converter.setPayloadAsBytes(true);
        adapter.setConverter(converter);
        adapter.setQos(1);
        adapter.setOutputChannel(mqttInputChannel);
        return adapter;
    }
}
