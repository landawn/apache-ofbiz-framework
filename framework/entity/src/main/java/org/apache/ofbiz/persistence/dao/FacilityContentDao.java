package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.FacilityContentEntity;

public interface FacilityContentDao extends CrudDao<FacilityContentEntity, FacilityContentEntity, SqlBuilder.PSC, FacilityContentDao> {
}
