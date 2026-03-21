package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.BillingAccountEntity;

public interface BillingAccountDao extends CrudDao<BillingAccountEntity, String, SqlBuilder.PSC, BillingAccountDao>, DelegatorQueryDao {
}
