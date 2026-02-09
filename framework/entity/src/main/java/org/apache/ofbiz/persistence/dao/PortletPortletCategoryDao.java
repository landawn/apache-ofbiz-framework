package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.PortletPortletCategoryEntity;

public interface PortletPortletCategoryDao extends CrudDao<PortletPortletCategoryEntity, PortletPortletCategoryEntity, SQLBuilder.PSC, PortletPortletCategoryDao> {
}
