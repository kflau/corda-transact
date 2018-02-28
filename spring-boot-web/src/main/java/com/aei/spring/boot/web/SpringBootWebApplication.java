package com.aei.spring.boot.web;

import net.corda.client.rpc.CordaRPCClient;
import net.corda.client.rpc.CordaRPCClientConfiguration;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.utilities.NetworkHostAndPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class SpringBootWebApplication {

	private static final Logger logger = LoggerFactory.getLogger(SpringBootWebApplication.class);

	@Value("${com.aei.corda.rpc.host:}")
	private String cordaRPCHost;
	@Value("${com.aei.corda.rpc.user:}")
	private String user;
	@Value("${com.aei.corda.rpc.password:}")
	private String password;

	@Bean
	public CordaRPCOps cordaRPCOps() {
		CordaRPCOps proxy = null;
		NetworkHostAndPort nodeAddress = NetworkHostAndPort.parse(cordaRPCHost);
		CordaRPCClient client = new CordaRPCClient(nodeAddress, CordaRPCClientConfiguration.DEFAULT);
		try {
			proxy = client.start(user, password).getProxy();
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
		}
		return proxy;
	}

	public static void main(String[] args) {
		SpringApplication.run(SpringBootWebApplication.class, args);
	}
}
