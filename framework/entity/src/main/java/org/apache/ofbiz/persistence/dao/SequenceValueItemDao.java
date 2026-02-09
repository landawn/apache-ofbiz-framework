package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.SequenceValueItemEntity;

public interface SequenceValueItemDao extends CrudDao<SequenceValueItemEntity, String, SQLBuilder.PSC, SequenceValueItemDao> {
}
