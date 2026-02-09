package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.MaritalStatusTypeEntity;

public interface MaritalStatusTypeDao extends CrudDao<MaritalStatusTypeEntity, String, SQLBuilder.PSC, MaritalStatusTypeDao> {
}
