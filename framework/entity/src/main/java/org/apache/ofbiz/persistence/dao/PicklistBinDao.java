package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.PicklistBinEntity;

public interface PicklistBinDao extends CrudDao<PicklistBinEntity, String, SqlBuilder.PSC, PicklistBinDao> {
}
