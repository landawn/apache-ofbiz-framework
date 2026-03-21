package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.DesiredFeatureEntity;

public interface DesiredFeatureDao extends CrudDao<DesiredFeatureEntity, DesiredFeatureEntity, SqlBuilder.PSC, DesiredFeatureDao> {
}
