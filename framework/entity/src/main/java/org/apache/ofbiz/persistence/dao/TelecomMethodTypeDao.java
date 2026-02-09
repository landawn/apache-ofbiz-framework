package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.TelecomMethodTypeEntity;

public interface TelecomMethodTypeDao extends CrudDao<TelecomMethodTypeEntity, String, SQLBuilder.PSC, TelecomMethodTypeDao> {
}
