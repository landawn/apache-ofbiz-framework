package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.BenefitTypeEntity;

public interface BenefitTypeDao extends CrudDao<BenefitTypeEntity, String, SqlBuilder.PSC, BenefitTypeDao> {
}
