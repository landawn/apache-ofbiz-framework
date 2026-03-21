package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.PartyBenefitEntity;

public interface PartyBenefitDao extends CrudDao<PartyBenefitEntity, PartyBenefitEntity, SqlBuilder.PSC, PartyBenefitDao> {
}
