package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.PayGradeEntity;

public interface PayGradeDao extends CrudDao<PayGradeEntity, String, SqlBuilder.PSC, PayGradeDao> {
}
