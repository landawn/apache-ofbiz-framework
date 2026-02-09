package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.UomTypeEntity;

public interface UomTypeDao extends CrudDao<UomTypeEntity, String, SQLBuilder.PSC, UomTypeDao> {
}
