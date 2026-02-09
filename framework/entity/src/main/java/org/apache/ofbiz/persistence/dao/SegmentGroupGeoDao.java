package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.SegmentGroupGeoEntity;

public interface SegmentGroupGeoDao extends CrudDao<SegmentGroupGeoEntity, SegmentGroupGeoEntity, SQLBuilder.PSC, SegmentGroupGeoDao> {
}
