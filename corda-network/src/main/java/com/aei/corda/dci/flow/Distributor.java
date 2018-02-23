package com.aei.corda.dci.flow;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.utilities.ProgressTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@InitiatingFlow
@StartableByRPC
public class Distributor extends FlowLogic<Void> {

    static private final Logger logger = LoggerFactory.getLogger(Distributor.class);

    private final ProgressTracker.Step DISTRIBUTE_INSTRUMENT = new ProgressTracker.Step("Distribute instrument.");

    private final ProgressTracker progressTracker = new ProgressTracker(
            DISTRIBUTE_INSTRUMENT
    );

    private List<Party> counterparties;
    private String instrument;

    public Distributor(List<Party> counterparties, String instrument) {
        this.counterparties = counterparties;
        this.instrument = instrument;
    }

    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    @Suspendable
    @Override
    public Void call() throws FlowException {
        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

        progressTracker.setCurrentStep(DISTRIBUTE_INSTRUMENT);
        for (Party counterparty : counterparties) {
            FlowSession counterpartySession = initiateFlow(counterparty);
            counterpartySession.send(instrument);
        }
        return null;
    }
}
