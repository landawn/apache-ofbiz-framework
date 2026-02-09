package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.PortalPagePortletEntity;

public interface PortalPagePortletDao extends CrudDao<PortalPagePortletEntity, PortalPagePortletEntity, SQLBuilder.PSC, PortalPagePortletDao> {
}
