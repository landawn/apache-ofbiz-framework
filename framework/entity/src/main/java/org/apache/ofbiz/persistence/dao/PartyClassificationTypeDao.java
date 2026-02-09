package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.PartyClassificationTypeEntity;

public interface PartyClassificationTypeDao extends CrudDao<PartyClassificationTypeEntity, String, SQLBuilder.PSC, PartyClassificationTypeDao> {
}
