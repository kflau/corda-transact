package com.aei.corda.dci.contract;

import com.aei.corda.dci.state.InstrumentState;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.contracts.Contract;
import net.corda.core.identity.AbstractParty;
import net.corda.core.transactions.LedgerTransaction;

import java.util.stream.Collectors;

import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;

public class InstrumentContract implements Contract {

    public static final String INSTRUMENT_CONTRACT_ID = "com.example.contract.InstrumentContract";

    @Override
    public void verify(LedgerTransaction tx) {
        final CommandWithParties<Commands.Create> command = requireSingleCommand(tx.getCommands(), Commands.Create.class);
        requireThat(require -> {
            require.using("No inputs should be consumed when issuing an Instrument.",
                    tx.getInputs().isEmpty());
            require.using("Only one output state should be created.",
                    tx.getOutputs().size() == 1);
            final InstrumentState out = tx.outputsOfType(InstrumentState.class).get(0);
            require.using("The seller and the buyer cannot be the same entity.",
                    out.getSeller() != out.getBuyer());
            require.using("All of the participants must be signers.",
                    command.getSigners().containsAll(out.getParticipants().stream().map(AbstractParty::getOwningKey).collect(Collectors.toList())));

            require.using("The Instrument's value must be non-empty.",
                    !out.getValue().isEmpty());

            return null;
        });
    }

    public interface Commands extends CommandData {
        class Create implements Commands {}
    }
}