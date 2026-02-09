package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ComponentEntity;

public interface ComponentDao extends CrudDao<ComponentEntity, String, SQLBuilder.PSC, ComponentDao> {
}
