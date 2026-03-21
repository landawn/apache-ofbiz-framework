package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.MaritalStatusEntity;

public interface MaritalStatusDao extends CrudDao<MaritalStatusEntity, MaritalStatusEntity, SqlBuilder.PSC, MaritalStatusDao> {
}
