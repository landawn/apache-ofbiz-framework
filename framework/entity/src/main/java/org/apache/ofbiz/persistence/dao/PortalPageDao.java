package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.PortalPageEntity;

public interface PortalPageDao extends CrudDao<PortalPageEntity, String, SqlBuilder.PSC, PortalPageDao> {
}
