package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.BillingAccountTermEntity;

public interface BillingAccountTermDao extends CrudDao<BillingAccountTermEntity, String, SqlBuilder.PSC, BillingAccountTermDao> {
}
