package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.QuoteRoleEntity;

public interface QuoteRoleDao extends CrudDao<QuoteRoleEntity, QuoteRoleEntity, SQLBuilder.PSC, QuoteRoleDao> {
}
