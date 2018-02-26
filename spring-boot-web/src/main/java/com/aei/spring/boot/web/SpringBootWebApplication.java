package com.aei.spring.boot.web;

import net.corda.client.rpc.CordaRPCClient;
import net.corda.client.rpc.CordaRPCClientConfiguration;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.utilities.NetworkHostAndPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class SpringBootWebApplication {

	@Value("${com.aei.corda.rpc.host:}")
	private String cordaRPCHost;

	@Bean
	public CordaRPCOps cordaRPCOps() {
		NetworkHostAndPort nodeAddress = NetworkHostAndPort.parse(cordaRPCHost);
		CordaRPCClient client = new CordaRPCClient(nodeAddress, CordaRPCClientConfiguration.DEFAULT);
		CordaRPCOps proxy = client.start("user1", "test").getProxy();
		return proxy;
	}

	public static void main(String[] args) {
		SpringApplication.run(SpringBootWebApplication.class, args);
	}
}
