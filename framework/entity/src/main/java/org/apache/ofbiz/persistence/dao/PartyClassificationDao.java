package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.PartyClassificationEntity;

public interface PartyClassificationDao extends CrudDao<PartyClassificationEntity, PartyClassificationEntity, SqlBuilder.PSC, PartyClassificationDao> {
}
