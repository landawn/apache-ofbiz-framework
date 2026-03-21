package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.EnumerationEntity;

public interface EnumerationDao extends CrudDao<EnumerationEntity, String, SqlBuilder.PSC, EnumerationDao> {
}
