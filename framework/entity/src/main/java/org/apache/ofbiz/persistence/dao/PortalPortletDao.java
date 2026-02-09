package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.PortalPortletEntity;

public interface PortalPortletDao extends CrudDao<PortalPortletEntity, String, SQLBuilder.PSC, PortalPortletDao> {
}
