package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.PayrollPreferenceEntity;

public interface PayrollPreferenceDao extends CrudDao<PayrollPreferenceEntity, PayrollPreferenceEntity, SqlBuilder.PSC, PayrollPreferenceDao> {
}
