package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.RecurrenceInfoEntity;

public interface RecurrenceInfoDao extends CrudDao<RecurrenceInfoEntity, String, SqlBuilder.PSC, RecurrenceInfoDao> {
}
