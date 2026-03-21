package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.BillingAccountTermAttrEntity;

public interface BillingAccountTermAttrDao extends CrudDao<BillingAccountTermAttrEntity, BillingAccountTermAttrEntity, SqlBuilder.PSC, BillingAccountTermAttrDao> {
}
