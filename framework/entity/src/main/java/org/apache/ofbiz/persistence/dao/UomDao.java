package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.UomEntity;

public interface UomDao extends CrudDao<UomEntity, String, SQLBuilder.PSC, UomDao> {
}
