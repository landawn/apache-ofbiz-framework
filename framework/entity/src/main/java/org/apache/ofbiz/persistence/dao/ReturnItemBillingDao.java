package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ReturnItemBillingEntity;

public interface ReturnItemBillingDao extends CrudDao<ReturnItemBillingEntity, ReturnItemBillingEntity, SQLBuilder.PSC, ReturnItemBillingDao> {
}
