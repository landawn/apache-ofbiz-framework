package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.EnumerationTypeEntity;

public interface EnumerationTypeDao extends CrudDao<EnumerationTypeEntity, String, SQLBuilder.PSC, EnumerationTypeDao> {
}
