package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.AgreementItemEntity;

public interface AgreementItemDao extends CrudDao<AgreementItemEntity, AgreementItemEntity, SqlBuilder.PSC, AgreementItemDao> {
}
