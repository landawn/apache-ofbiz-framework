package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.PicklistItemEntity;

public interface PicklistItemDao extends CrudDao<PicklistItemEntity, PicklistItemEntity, SQLBuilder.PSC, PicklistItemDao> {
}
