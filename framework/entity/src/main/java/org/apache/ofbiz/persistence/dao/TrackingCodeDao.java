package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.TrackingCodeEntity;

public interface TrackingCodeDao extends CrudDao<TrackingCodeEntity, String, SqlBuilder.PSC, TrackingCodeDao> {
}
