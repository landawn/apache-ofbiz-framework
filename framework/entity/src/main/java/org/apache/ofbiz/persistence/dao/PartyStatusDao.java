package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.PartyStatusEntity;

public interface PartyStatusDao extends CrudDao<PartyStatusEntity, PartyStatusEntity, SqlBuilder.PSC, PartyStatusDao> {
}
