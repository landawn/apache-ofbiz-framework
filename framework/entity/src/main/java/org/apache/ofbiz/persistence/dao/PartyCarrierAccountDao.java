package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.PartyCarrierAccountEntity;

public interface PartyCarrierAccountDao extends CrudDao<PartyCarrierAccountEntity, PartyCarrierAccountEntity, SQLBuilder.PSC, PartyCarrierAccountDao> {
}
