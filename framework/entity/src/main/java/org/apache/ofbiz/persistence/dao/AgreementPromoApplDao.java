package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.AgreementPromoApplEntity;

public interface AgreementPromoApplDao extends CrudDao<AgreementPromoApplEntity, AgreementPromoApplEntity, SqlBuilder.PSC, AgreementPromoApplDao> {
}
