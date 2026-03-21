package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.AgreementContentEntity;

public interface AgreementContentDao extends CrudDao<AgreementContentEntity, AgreementContentEntity, SqlBuilder.PSC, AgreementContentDao> {
}
