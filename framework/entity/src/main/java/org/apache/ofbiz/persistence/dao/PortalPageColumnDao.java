package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.PortalPageColumnEntity;

public interface PortalPageColumnDao extends CrudDao<PortalPageColumnEntity, PortalPageColumnEntity, SQLBuilder.PSC, PortalPageColumnDao> {
}
