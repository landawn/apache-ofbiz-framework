package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.TrackingCodeOrderReturnEntity;

public interface TrackingCodeOrderReturnDao extends CrudDao<TrackingCodeOrderReturnEntity, TrackingCodeOrderReturnEntity, SQLBuilder.PSC, TrackingCodeOrderReturnDao> {
}
