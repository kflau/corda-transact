package com.aei.spring.boot.web.controller;

import com.aei.corda.dci.flow.Buyer;
import com.aei.corda.dci.flow.Distributor;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import joptsimple.internal.Strings;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.messaging.FlowProgressHandle;
import net.corda.core.transactions.SignedTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.util.List;

@RestController
@RequestMapping("/api/instrument")
public class InstrumentCommandController {

    private static final Logger logger = LoggerFactory.getLogger(InstrumentCommandController.class);
    private CordaX500Name myLegalName;
    private List<String> serviceNames;

    @Autowired(required = false)
    private CordaRPCOps rpcOps;

    @PostConstruct
    public void init() {
        if (rpcOps != null) {
            myLegalName = rpcOps.nodeInfo().getLegalIdentities().get(0).getName();
        }
        serviceNames = ImmutableList.of("Controller", "Network Map Service");
    }

    @RequestMapping(value = "/distribute", method = RequestMethod.PUT)
    public ResponseEntity<String> distribute(
            @RequestParam("instrument") String instrument,
            @RequestParam("partyName") String partyName) {
        Preconditions.checkNotNull(rpcOps, "CordaRPC not enabled");
        if (Strings.isNullOrEmpty(instrument)) {
            return ResponseEntity.badRequest().body("Query parameter 'instrument' must be non-negative.");
        }
        if (Strings.isNullOrEmpty(partyName)) {
            return ResponseEntity.badRequest().body("Query parameter 'partyName' missing or has wrong format.");
        }
        Party counterparty = rpcOps.wellKnownPartyFromX500Name(CordaX500Name.parse(partyName));
        if (counterparty == null) {
            return ResponseEntity.badRequest().body(String.format("Party named %s cannot be found.", partyName));
        }
        try {
            rpcOps.startTrackedFlowDynamic(Distributor.class, ImmutableList.of(counterparty), instrument);

            String msg = String.format("Distributed %s to %s.", instrument, partyName);
            return ResponseEntity.ok(msg);

        } catch (Throwable ex) {
            String msg = ex.getMessage();
            logger.error(ex.getMessage(), ex);
            return ResponseEntity.badRequest().body(msg);
        }
    }

    @RequestMapping(value = "/order", method = RequestMethod.PUT)
    public ResponseEntity<String> order(
            @RequestParam("instrument") String instrument,
            @RequestParam("partyName") String partyName) {
        Preconditions.checkNotNull(rpcOps, "CordaRPC not enabled");
        if (Strings.isNullOrEmpty(instrument)) {
            return ResponseEntity.badRequest().body("Query parameter 'instrument' must be non-negative.");
        }
        if (Strings.isNullOrEmpty(partyName)) {
            return ResponseEntity.badRequest().body("Query parameter 'partyName' missing or has wrong format.");
        }
        Party counterparty = rpcOps.wellKnownPartyFromX500Name(CordaX500Name.parse(partyName));
        if (counterparty == null) {
            return ResponseEntity.badRequest().body(String.format("Party named %s cannot be found", partyName));
        }
        try {
            FlowProgressHandle<SignedTransaction> flowHandle = rpcOps.startTrackedFlowDynamic(Buyer.class, counterparty, instrument);
            flowHandle.getProgress().subscribe(evt -> logger.info(">> {}", evt));

            SignedTransaction result = flowHandle
                    .getReturnValue()
                    .get();

            String msg = String.format("Transaction id %s committed to ledger.", result.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(msg);
        } catch (Throwable ex) {
            String msg = ex.getMessage();
            logger.error(ex.getMessage(), ex);
            return ResponseEntity.badRequest().body(msg);
        }
    }
}
