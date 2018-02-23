package com.aei.corda.dci.state;

import com.aei.corda.dci.schema.InstrumentSchemaV1;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.schemas.MappedSchema;
import net.corda.core.schemas.PersistentState;
import net.corda.core.schemas.QueryableState;

import java.util.List;

public class InstrumentState implements LinearState, QueryableState {
    private final String value;
    private final Party seller;
    private final Party buyer;
    private final UniqueIdentifier linearId;

    public InstrumentState(String value, Party seller, Party buyer) {
        this.value = Optional.fromNullable(value).or("");
        this.seller = seller;
        this.buyer = buyer;
        this.linearId = new UniqueIdentifier();
    }

    public String getValue() { return value; }

    public Party getSeller() {
        return seller;
    }

    public Party getBuyer() {
        return buyer;
    }

    @Override public UniqueIdentifier getLinearId() { return linearId; }
    @Override public List<AbstractParty> getParticipants() {
        return ImmutableList.of(seller, buyer);
    }

    @Override public PersistentState generateMappedObject(MappedSchema schema) {
        if (schema instanceof InstrumentSchemaV1) {
            return new InstrumentSchemaV1.PersistentInstrument(
                    this.value,
                    this.seller.getName().toString(),
                    this.buyer.getName().toString(),
                    this.linearId.getId());
        } else {
            throw new IllegalArgumentException("Unrecognised schema $schema");
        }
    }

    @Override public Iterable<MappedSchema> supportedSchemas() {
        return ImmutableList.of(new InstrumentSchemaV1());
    }

    @Override
    public String toString() {
        return String.format("%s(instrument=%s, seller=%s, buyer=%s, linearId=%s)", getClass().getSimpleName(), value, seller, buyer, linearId);
    }
}