package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.AgreementContentEntity;

public interface AgreementContentDao extends CrudDao<AgreementContentEntity, AgreementContentEntity, SQLBuilder.PSC, AgreementContentDao> {
}
