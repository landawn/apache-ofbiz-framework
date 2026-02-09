package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.MaritalStatusEntity;

public interface MaritalStatusDao extends CrudDao<MaritalStatusEntity, MaritalStatusEntity, SQLBuilder.PSC, MaritalStatusDao> {
}
