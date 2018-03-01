package com.aei.corda.dci.flow;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.base.Preconditions;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.FlowSession;
import net.corda.core.flows.InitiatedBy;
import net.corda.core.identity.Party;
import net.corda.core.node.ServiceHub;
import net.corda.core.utilities.ProgressTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@InitiatedBy(Seller.class)
public class Bookkeeper extends FlowLogic<Void> {

    private static final Logger logger = LoggerFactory.getLogger(Bookkeeper.class);

    private ProgressTracker.Step BOOKKEEPING = new ProgressTracker.Step("Bookkeeping transaction.");

    private ProgressTracker progressTracker = new ProgressTracker(
            BOOKKEEPING
    );

    private FlowSession counterpartySession;

    public Bookkeeper(FlowSession counterpartySession) {
        this.counterpartySession = counterpartySession;
    }

    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    @Suspendable
    @Override
    public Void call() throws FlowException {
        ServiceHub serviceHub = getServiceHub();
        Party notary = serviceHub.getNetworkMapCache().getNotaryIdentities().get(0);

        progressTracker.setCurrentStep(BOOKKEEPING);
        String instrument = counterpartySession.receive(String.class).unwrap(data -> data);
        Preconditions.checkNotNull(instrument, "Seller receives order for nothing");

        logger.debug("Received {} for bookkeeping", instrument);
        return null;
    }
}
