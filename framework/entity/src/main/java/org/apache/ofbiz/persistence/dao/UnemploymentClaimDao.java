package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.UnemploymentClaimEntity;

public interface UnemploymentClaimDao extends CrudDao<UnemploymentClaimEntity, String, SQLBuilder.PSC, UnemploymentClaimDao> {
}
