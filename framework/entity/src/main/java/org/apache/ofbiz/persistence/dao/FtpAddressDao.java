package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.FtpAddressEntity;

public interface FtpAddressDao extends CrudDao<FtpAddressEntity, String, SqlBuilder.PSC, FtpAddressDao>, DelegatorQueryDao {
}
