package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.MetaDataPredicateEntity;

public interface MetaDataPredicateDao extends CrudDao<MetaDataPredicateEntity, String, SqlBuilder.PSC, MetaDataPredicateDao> {
}
