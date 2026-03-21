package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.CustRequestResolutionEntity;

public interface CustRequestResolutionDao extends CrudDao<CustRequestResolutionEntity, String, SqlBuilder.PSC, CustRequestResolutionDao> {
}
