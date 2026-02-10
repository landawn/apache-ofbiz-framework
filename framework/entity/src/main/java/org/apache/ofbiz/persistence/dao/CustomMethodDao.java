package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.CustomMethodEntity;

public interface CustomMethodDao extends CrudDao<CustomMethodEntity, String, SQLBuilder.PSC, CustomMethodDao>, DelegatorQueryDao {
}
