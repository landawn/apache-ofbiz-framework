package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.PartyQualEntity;

public interface PartyQualDao extends CrudDao<PartyQualEntity, PartyQualEntity, SqlBuilder.PSC, PartyQualDao> {
}
