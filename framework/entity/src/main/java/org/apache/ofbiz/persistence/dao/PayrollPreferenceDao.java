package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.PayrollPreferenceEntity;

public interface PayrollPreferenceDao extends CrudDao<PayrollPreferenceEntity, PayrollPreferenceEntity, SQLBuilder.PSC, PayrollPreferenceDao> {
}
