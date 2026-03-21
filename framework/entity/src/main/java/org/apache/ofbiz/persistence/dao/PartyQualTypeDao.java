package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.PartyQualTypeEntity;

public interface PartyQualTypeDao extends CrudDao<PartyQualTypeEntity, String, SqlBuilder.PSC, PartyQualTypeDao> {
}
