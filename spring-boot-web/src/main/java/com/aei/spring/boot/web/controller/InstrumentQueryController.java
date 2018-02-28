package com.aei.spring.boot.web.controller;

import com.aei.corda.dci.state.InstrumentState;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.node.NodeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

@RestController
@RequestMapping("/api/instrument")
public class InstrumentQueryController {

    private static final Logger logger = LoggerFactory.getLogger(InstrumentQueryController.class);
    private CordaX500Name myLegalName;
    private List<String> serviceNames;

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired(required = false)
    private CordaRPCOps rpcOps;

    @PostConstruct
    public void init() {
        if (rpcOps != null) {
            myLegalName = rpcOps.nodeInfo().getLegalIdentities().get(0).getName();
        }
        serviceNames = ImmutableList.of("Controller", "Network Map Service");
    }

    @RequestMapping(value = "/me", method = RequestMethod.GET)
    public Map<String, CordaX500Name> whoami() {
        Preconditions.checkNotNull(rpcOps, "CordaRPC not enabled");
        return ImmutableMap.of("me", myLegalName);
    }

    @RequestMapping(value = "/peers", method = RequestMethod.GET)
    public Map<String, List<CordaX500Name>> getPeers() {
        Preconditions.checkNotNull(rpcOps, "CordaRPC not enabled");
        List<NodeInfo> nodeInfoSnapshot = rpcOps.networkMapSnapshot();
        return ImmutableMap.of("peers", nodeInfoSnapshot
                .stream()
                .map(node -> node.getLegalIdentities().get(0).getName())
                .filter(name -> !name.equals(myLegalName) && !serviceNames.contains(name.getOrganisation()))
                .collect(toList()));
    }

    @RequestMapping(value = "/instruments", method = RequestMethod.GET)
    public List<JsonNode> getInstrumentStates() {
        Preconditions.checkNotNull(rpcOps, "CordaRPC not enabled");
        List<StateAndRef<InstrumentState>> states = rpcOps.vaultQuery(InstrumentState.class).getStates();
        return states.stream().map(instrumentStateStateAndRef -> {
            ObjectNode objectNode = objectMapper.createObjectNode();
            objectNode.put("ref", instrumentStateStateAndRef.getRef().toString());
            objectNode.put("state", instrumentStateStateAndRef.getState().toString());
            return objectNode;
        }).collect(Collectors.toList());
    }
}
