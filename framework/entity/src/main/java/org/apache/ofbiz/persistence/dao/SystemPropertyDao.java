package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.SystemPropertyEntity;

public interface SystemPropertyDao extends CrudDao<SystemPropertyEntity, SystemPropertyEntity, SQLBuilder.PSC, SystemPropertyDao> {
}
