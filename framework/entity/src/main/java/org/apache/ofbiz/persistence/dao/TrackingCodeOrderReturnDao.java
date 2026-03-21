package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.TrackingCodeOrderReturnEntity;

public interface TrackingCodeOrderReturnDao extends CrudDao<TrackingCodeOrderReturnEntity, TrackingCodeOrderReturnEntity, SqlBuilder.PSC, TrackingCodeOrderReturnDao> {
}
