package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.GoodIdentificationTypeEntity;

public interface GoodIdentificationTypeDao extends CrudDao<GoodIdentificationTypeEntity, String, SqlBuilder.PSC, GoodIdentificationTypeDao> {
}
