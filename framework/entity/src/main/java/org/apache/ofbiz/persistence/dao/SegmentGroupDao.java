package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.SegmentGroupEntity;

public interface SegmentGroupDao extends CrudDao<SegmentGroupEntity, String, SQLBuilder.PSC, SegmentGroupDao> {
}
