package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.SettlementTermEntity;

public interface SettlementTermDao extends CrudDao<SettlementTermEntity, String, SQLBuilder.PSC, SettlementTermDao> {
}
