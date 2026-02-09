package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.PartyRateNewEntity;

public interface PartyRateNewDao extends CrudDao<PartyRateNewEntity, PartyRateNewEntity, SQLBuilder.PSC, PartyRateNewDao> {
}
