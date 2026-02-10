package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.GoodIdentificationEntity;

public interface GoodIdentificationDao
        extends CrudDao<GoodIdentificationEntity, GoodIdentificationEntity, SQLBuilder.PSC, GoodIdentificationDao>, DelegatorQueryDao {
}
