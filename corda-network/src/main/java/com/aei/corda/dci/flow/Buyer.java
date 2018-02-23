package com.aei.corda.dci.flow;

import co.paralleluniverse.fibers.Suspendable;
import com.aei.corda.dci.state.InstrumentState;
import com.google.common.base.Strings;
import net.corda.confidential.IdentitySyncFlow;
import net.corda.core.contracts.ContractState;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.utilities.ProgressTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;

import static net.corda.core.contracts.ContractsDSL.requireThat;

@InitiatingFlow
@StartableByRPC
public class Buyer extends FlowLogic<SignedTransaction> {

    static private final Logger logger = LoggerFactory.getLogger(Buyer.class);

    private final ProgressTracker.Step PLACE_ORDER = new ProgressTracker.Step("Place order.");
    private final ProgressTracker.Step ORDER_CONFIRM = new ProgressTracker.Step("Confirm order.");
    private final ProgressTracker.Step AWAIT_SIGNED_TRANSACTION = new ProgressTracker.Step("Place order.");
    private final ProgressTracker.Step SIGNING_TRANSACTION = new ProgressTracker.Step("Signing transaction with our private key.");

    private final ProgressTracker progressTracker = new ProgressTracker(
            PLACE_ORDER,
            ORDER_CONFIRM,
            AWAIT_SIGNED_TRANSACTION,
            SIGNING_TRANSACTION
    );

    private Party counterparty;
    private String instrument;

    public Buyer(Party counterparty, String instrument) {
        this.counterparty = counterparty;
        this.instrument = instrument;
    }

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {
        progressTracker.setCurrentStep(PLACE_ORDER);
        FlowSession counterpartySession = initiateFlow(counterparty);
        counterpartySession.send(instrument);

        progressTracker.setCurrentStep(ORDER_CONFIRM);
        LinkedHashMap<String, Object> spotsAndStrikes = counterpartySession.receive(LinkedHashMap.class).unwrap(data -> data);
        counterpartySession.send("ok");

        progressTracker.setCurrentStep(AWAIT_SIGNED_TRANSACTION);
        subFlow(new IdentitySyncFlow.Receive(counterpartySession));

        progressTracker.setCurrentStep(SIGNING_TRANSACTION);
        class SignTxFlow extends SignTransactionFlow {
            private SignTxFlow(FlowSession counterpartyFlow, ProgressTracker progressTracker) {
                super(counterpartyFlow, progressTracker);
            }

            @Override
            protected void checkTransaction(SignedTransaction stx) {
                requireThat(require -> {
                    ContractState output = stx.getTx().getOutputs().get(0).getData();
                    require.using("This must be an Instrument transaction.", output instanceof InstrumentState);
                    InstrumentState instrumentState = (InstrumentState) output;
                    require.using("Instrument States without a value are not accepted.",
                            !Strings.isNullOrEmpty(instrumentState.getValue()));
                    return null;
                });
            }
        }

        return subFlow(new SignTxFlow(counterpartySession, SignTransactionFlow.Companion.tracker()));
    }
}
