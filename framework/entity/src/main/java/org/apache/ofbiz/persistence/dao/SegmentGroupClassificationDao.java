package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.SegmentGroupClassificationEntity;

public interface SegmentGroupClassificationDao extends CrudDao<SegmentGroupClassificationEntity, SegmentGroupClassificationEntity, SQLBuilder.PSC, SegmentGroupClassificationDao> {
}
