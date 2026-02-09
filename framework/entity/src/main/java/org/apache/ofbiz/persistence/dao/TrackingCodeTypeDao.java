package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.TrackingCodeTypeEntity;

public interface TrackingCodeTypeDao extends CrudDao<TrackingCodeTypeEntity, String, SQLBuilder.PSC, TrackingCodeTypeDao> {
}
