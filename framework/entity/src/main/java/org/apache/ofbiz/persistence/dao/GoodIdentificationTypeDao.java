package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.GoodIdentificationTypeEntity;

public interface GoodIdentificationTypeDao extends CrudDao<GoodIdentificationTypeEntity, String, SQLBuilder.PSC, GoodIdentificationTypeDao> {
}
