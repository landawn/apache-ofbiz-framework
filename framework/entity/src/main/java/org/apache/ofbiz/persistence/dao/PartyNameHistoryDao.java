package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.PartyNameHistoryEntity;

public interface PartyNameHistoryDao extends CrudDao<PartyNameHistoryEntity, PartyNameHistoryEntity, SQLBuilder.PSC, PartyNameHistoryDao> {
}
