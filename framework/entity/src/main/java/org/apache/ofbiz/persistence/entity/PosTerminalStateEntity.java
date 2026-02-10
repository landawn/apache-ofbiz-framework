package org.apache.ofbiz.persistence.entity;

import java.sql.Timestamp;

import com.landawn.abacus.annotation.Column;
import com.landawn.abacus.annotation.Entity;
import com.landawn.abacus.annotation.Id;
import com.landawn.abacus.annotation.Table;

@Entity(name = "POS_TERMINAL_STATE")
@Table(name = "POS_TERMINAL_STATE")
public class PosTerminalStateEntity {

    @Id
    @Column(name = "POS_TERMINAL_ID")
    private String posTerminalId;

    @Id
    @Column(name = "OPENED_DATE")
    private Timestamp openedDate;

    @Column(name = "CLOSED_DATE")
    private Timestamp closedDate;

    public String getPosTerminalId() {
        return posTerminalId;
    }

    public void setPosTerminalId(String posTerminalId) {
        this.posTerminalId = posTerminalId;
    }

    public Timestamp getOpenedDate() {
        return openedDate;
    }

    public void setOpenedDate(Timestamp openedDate) {
        this.openedDate = openedDate;
    }

    public Timestamp getClosedDate() {
        return closedDate;
    }

    public void setClosedDate(Timestamp closedDate) {
        this.closedDate = closedDate;
    }
}
