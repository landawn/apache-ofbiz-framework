package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.PartyCarrierAccountEntity;

public interface PartyCarrierAccountDao extends CrudDao<PartyCarrierAccountEntity, PartyCarrierAccountEntity, SqlBuilder.PSC, PartyCarrierAccountDao> {
}
