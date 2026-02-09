package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.AgreementPartyApplicEntity;

public interface AgreementPartyApplicDao extends CrudDao<AgreementPartyApplicEntity, AgreementPartyApplicEntity, SQLBuilder.PSC, AgreementPartyApplicDao> {
}
