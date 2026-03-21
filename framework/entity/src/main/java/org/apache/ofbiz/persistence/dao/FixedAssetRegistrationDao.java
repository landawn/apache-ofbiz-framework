package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.FixedAssetRegistrationEntity;

public interface FixedAssetRegistrationDao extends CrudDao<FixedAssetRegistrationEntity, FixedAssetRegistrationEntity, SqlBuilder.PSC, FixedAssetRegistrationDao> {
}
