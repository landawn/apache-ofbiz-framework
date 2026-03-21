package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.PartyResumeEntity;

public interface PartyResumeDao extends CrudDao<PartyResumeEntity, String, SqlBuilder.PSC, PartyResumeDao> {
}
