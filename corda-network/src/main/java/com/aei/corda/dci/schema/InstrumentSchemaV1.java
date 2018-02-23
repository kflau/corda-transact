package com.aei.corda.dci.schema;

import com.google.common.collect.ImmutableList;
import net.corda.core.schemas.MappedSchema;
import net.corda.core.schemas.PersistentState;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.UUID;

public class InstrumentSchemaV1 extends MappedSchema {
    public InstrumentSchemaV1() {
        super(InstrumentSchema.class, 1, ImmutableList.of(PersistentInstrument.class));
    }

    @Entity
    @Table(name = "instrument_states")
    public static class PersistentInstrument extends PersistentState {
        @Column(name = "value") private final String value;
        @Column(name = "seller") private final String seller;
        @Column(name = "buyer") private final String buyer;
        @Column(name = "linear_id") private final UUID linearId;


        public PersistentInstrument(String value, String seller, String buyer, UUID linearId) {
            this.value = value;
            this.seller = seller;
            this.buyer = buyer;
            this.linearId = linearId;
        }

        // Default constructor required by hibernate.
        public PersistentInstrument() {
            this.value = "";
            this.seller = null;
            this.buyer = null;
            this.linearId = null;
        }

        public String getSeller() {
            return seller;
        }

        public String getBuyer() {
            return buyer;
        }

        public String getValue() {
            return value;
        }

        public UUID getId() {
            return linearId;
        }
    }
}