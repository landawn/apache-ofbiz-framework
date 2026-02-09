package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.CustRequestPartyEntity;

public interface CustRequestPartyDao extends CrudDao<CustRequestPartyEntity, CustRequestPartyEntity, SQLBuilder.PSC, CustRequestPartyDao> {
}
