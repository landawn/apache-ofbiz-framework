package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.TrackingCodeOrderEntity;

public interface TrackingCodeOrderDao extends CrudDao<TrackingCodeOrderEntity, TrackingCodeOrderEntity, SQLBuilder.PSC, TrackingCodeOrderDao> {
}
