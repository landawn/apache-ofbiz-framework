package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.AffiliateEntity;

public interface AffiliateDao extends CrudDao<AffiliateEntity, String, SqlBuilder.PSC, AffiliateDao> {
}
