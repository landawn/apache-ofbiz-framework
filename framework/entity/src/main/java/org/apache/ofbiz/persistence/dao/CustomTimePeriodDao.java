package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.CustomTimePeriodEntity;

public interface CustomTimePeriodDao extends CrudDao<CustomTimePeriodEntity, String, SQLBuilder.PSC, CustomTimePeriodDao> {
}
