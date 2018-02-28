package com.aei.corda.dci.flow;

import co.paralleluniverse.fibers.Suspendable;
import com.aei.corda.dci.contract.InstrumentContract;
import com.aei.corda.dci.oracle.FxRateOracle;
import com.aei.corda.dci.state.InstrumentState;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import net.corda.confidential.IdentitySyncFlow;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.StateAndContract;
import net.corda.core.flows.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.node.NodeInfo;
import net.corda.core.node.ServiceHub;
import net.corda.core.node.services.NetworkMapCache;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@InitiatingFlow
@InitiatedBy(Buyer.class)
public class Seller extends FlowLogic<SignedTransaction> {

    private static final Logger logger = LoggerFactory.getLogger(Seller.class);
    private static final String BOOKKEEPER_NODENAME = "O=Bookkeeper,L=London,C=GB";

    private ProgressTracker.Step DISTRIBUTE_INSTRUMENT = new ProgressTracker.Step("Distribute instrument.");
    private ProgressTracker.Step PENDING_PLACE_ORDER = new ProgressTracker.Step("Pending place order.");
    private ProgressTracker.Step ORDER_PLACED = new ProgressTracker.Step("Pending place order.");
    private ProgressTracker.Step ORDER_CONFIRM = new ProgressTracker.Step("Confirm order.");
    private ProgressTracker.Step GENERATING_TRANSACTION = new ProgressTracker.Step("Generating transaction.");
    private ProgressTracker.Step SIGNING_TRANSACTION = new ProgressTracker.Step("Signing transaction with our private key.");
    private ProgressTracker.Step NOTIFY_SIGNED_TRANSACTION = new ProgressTracker.Step("Notify counteryparties about signed transaction.");
    private ProgressTracker.Step VERIFYING_TRANSACTION = new ProgressTracker.Step("Verifying contract constraints.");
    private ProgressTracker.Step BOOKKEEPING = new ProgressTracker.Step("Bookkeeping transaction.");
    private ProgressTracker.Step GATHERING_SIGS = new ProgressTracker.Step("Gathering the counterparty's signature.") {
        @Override public ProgressTracker childProgressTracker() {
            return CollectSignaturesFlow.Companion.tracker();
        }
    };
    private ProgressTracker.Step FINALISING_TRANSACTION = new ProgressTracker.Step("Obtaining notary signature and recording transaction.") {
        @Override public ProgressTracker childProgressTracker() {
            return FinalityFlow.Companion.tracker();
        }
    };

    private ProgressTracker progressTracker = new ProgressTracker(
            DISTRIBUTE_INSTRUMENT,
            PENDING_PLACE_ORDER,
            ORDER_PLACED,
            ORDER_CONFIRM,
            GENERATING_TRANSACTION,
            SIGNING_TRANSACTION,
            NOTIFY_SIGNED_TRANSACTION,
            VERIFYING_TRANSACTION,
            GATHERING_SIGS,
            BOOKKEEPING,
            FINALISING_TRANSACTION
    );

    private FlowSession counterpartySession;

    public Seller(FlowSession counterpartySession) {
        this.counterpartySession = counterpartySession;
    }

    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {
        try {
            ServiceHub serviceHub = getServiceHub();
            NetworkMapCache networkMapCache = serviceHub.getNetworkMapCache();
            List<NodeInfo> allNodes = networkMapCache.getAllNodes();
            Party notary = networkMapCache.getNotaryIdentities().get(0);
            NodeInfo myInfo = serviceHub.getMyInfo();
            List<Party> legalIdentities = myInfo.getLegalIdentities();

            progressTracker.setCurrentStep(ORDER_PLACED);
            String instrument = counterpartySession.receive(String.class).unwrap(data -> data);
            Preconditions.checkNotNull(instrument, "Buyer placed order for nothing");

            progressTracker.setCurrentStep(ORDER_CONFIRM);
            FxRateOracle fxRateOracle = serviceHub.cordaService(FxRateOracle.class);

            List<BigDecimal> spots = fxRateOracle.query();
            List<BigDecimal> strikes = fxRateOracle.query();
            LinkedHashMap<String, Object> parameters = Maps.newLinkedHashMap();
            parameters.put("spots", spots);
            parameters.put("strikes", strikes);
            logger.debug("sending parameters to counterparty: {}", parameters);
            String confirmMessage = counterpartySession.sendAndReceive(String.class, parameters).unwrap(data -> data);
            Preconditions.checkState("ok".equalsIgnoreCase(confirmMessage), "Confirm message is not OK");

            progressTracker.setCurrentStep(GENERATING_TRANSACTION);
            InstrumentState instrumentState = new InstrumentState(
                    instrument,
                    legalIdentities.get(0),
                    counterpartySession.getCounterparty());
            Command<InstrumentContract.Commands.Create> txCommand = new Command<>(
                    new InstrumentContract.Commands.Create(),
                    instrumentState.getParticipants().stream().map(AbstractParty::getOwningKey).collect(Collectors.toList()));
            TransactionBuilder txBuilder = new TransactionBuilder(notary).withItems(
                    new StateAndContract(instrumentState, InstrumentContract.INSTRUMENT_CONTRACT_ID),
                    txCommand);

            progressTracker.setCurrentStep(VERIFYING_TRANSACTION);
            txBuilder.verify(serviceHub);

            progressTracker.setCurrentStep(SIGNING_TRANSACTION);
            SignedTransaction partSignedTx = serviceHub.signInitialTransaction(txBuilder);

            progressTracker.setCurrentStep(NOTIFY_SIGNED_TRANSACTION);
            subFlow(new IdentitySyncFlow.Send(counterpartySession, txBuilder.toWireTransaction(serviceHub)));

            progressTracker.setCurrentStep(GATHERING_SIGS);
            SignedTransaction fullySignedTx = subFlow(new CollectSignaturesFlow(
                    partSignedTx,
                    Sets.newHashSet(counterpartySession),
                    CollectSignaturesFlow.Companion.tracker()));

            progressTracker.setCurrentStep(BOOKKEEPING);
            Optional<Party> bookkeeperOptional = allNodes.stream()
                    .filter(nodeInfo -> CordaX500Name.parse(BOOKKEEPER_NODENAME).equals(
                            nodeInfo.getLegalIdentities().get(0).getName()))
                    .map(nodeInfo -> nodeInfo.getLegalIdentities().get(0))
                    .findFirst();
            if (bookkeeperOptional.isPresent()) {
                Party bookkeeper = bookkeeperOptional.get();
                FlowSession bookkeeperSession = initiateFlow(bookkeeper);
                bookkeeperSession.send(instrument);
            }

            progressTracker.setCurrentStep(FINALISING_TRANSACTION);
            return subFlow(new FinalityFlow(fullySignedTx));
        } catch (Exception ex) {
            throw new FlowException(ex);
        }
    }
}
