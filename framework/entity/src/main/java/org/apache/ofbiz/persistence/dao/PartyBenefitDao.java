package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.PartyBenefitEntity;

public interface PartyBenefitDao extends CrudDao<PartyBenefitEntity, PartyBenefitEntity, SQLBuilder.PSC, PartyBenefitDao> {
}
