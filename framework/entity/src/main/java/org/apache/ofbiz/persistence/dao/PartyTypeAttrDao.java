package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.PartyTypeAttrEntity;

public interface PartyTypeAttrDao extends CrudDao<PartyTypeAttrEntity, PartyTypeAttrEntity, SqlBuilder.PSC, PartyTypeAttrDao> {
}
