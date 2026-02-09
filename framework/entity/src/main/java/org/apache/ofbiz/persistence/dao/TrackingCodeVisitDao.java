package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.TrackingCodeVisitEntity;

public interface TrackingCodeVisitDao extends CrudDao<TrackingCodeVisitEntity, TrackingCodeVisitEntity, SQLBuilder.PSC, TrackingCodeVisitDao> {
}
