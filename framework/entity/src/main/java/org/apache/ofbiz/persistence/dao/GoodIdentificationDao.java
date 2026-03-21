package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.GoodIdentificationEntity;

public interface GoodIdentificationDao
        extends CrudDao<GoodIdentificationEntity, GoodIdentificationEntity, SqlBuilder.PSC, GoodIdentificationDao>, DelegatorQueryDao {
}
