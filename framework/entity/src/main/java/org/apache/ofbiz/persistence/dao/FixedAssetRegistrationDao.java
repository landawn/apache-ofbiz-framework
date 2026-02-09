package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.FixedAssetRegistrationEntity;

public interface FixedAssetRegistrationDao extends CrudDao<FixedAssetRegistrationEntity, FixedAssetRegistrationEntity, SQLBuilder.PSC, FixedAssetRegistrationDao> {
}
