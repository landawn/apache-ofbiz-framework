package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ReturnItemBillingEntity;

public interface ReturnItemBillingDao extends CrudDao<ReturnItemBillingEntity, ReturnItemBillingEntity, SqlBuilder.PSC, ReturnItemBillingDao> {
}
