package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.PicklistStatusEntity;

public interface PicklistStatusDao extends CrudDao<PicklistStatusEntity, PicklistStatusEntity, SQLBuilder.PSC, PicklistStatusDao> {
}
