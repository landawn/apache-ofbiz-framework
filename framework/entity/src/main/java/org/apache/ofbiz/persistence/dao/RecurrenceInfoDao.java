package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.RecurrenceInfoEntity;

public interface RecurrenceInfoDao extends CrudDao<RecurrenceInfoEntity, String, SQLBuilder.PSC, RecurrenceInfoDao> {
}
