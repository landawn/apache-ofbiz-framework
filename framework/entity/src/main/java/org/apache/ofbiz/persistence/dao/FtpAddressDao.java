package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.FtpAddressEntity;

public interface FtpAddressDao extends CrudDao<FtpAddressEntity, String, SQLBuilder.PSC, FtpAddressDao>, DelegatorQueryDao {
}
