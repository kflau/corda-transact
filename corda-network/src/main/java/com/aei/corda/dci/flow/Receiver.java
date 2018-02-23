package com.aei.corda.dci.flow;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.FlowSession;
import net.corda.core.flows.InitiatedBy;
import net.corda.core.identity.Party;
import net.corda.core.utilities.ProgressTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@InitiatedBy(Distributor.class)
public class Receiver extends FlowLogic<String> {

    static private final Logger logger = LoggerFactory.getLogger(Receiver.class);

    private final ProgressTracker.Step RECEIVE_INSTRUMENT = new ProgressTracker.Step("Receive instrument.");

    private final ProgressTracker progressTracker = new ProgressTracker(
            RECEIVE_INSTRUMENT
    );

    private FlowSession counterpartySession;

    public Receiver(FlowSession counterpartySession) {
        this.counterpartySession = counterpartySession;
    }

    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    @Suspendable
    @Override
    public String call() throws FlowException {
        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

        progressTracker.setCurrentStep(RECEIVE_INSTRUMENT);
        return counterpartySession.receive(String.class).unwrap(data -> data);
    }
}
