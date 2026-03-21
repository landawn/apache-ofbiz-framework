package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.EmailAddressVerificationEntity;

public interface EmailAddressVerificationDao extends CrudDao<EmailAddressVerificationEntity, String, SqlBuilder.PSC, EmailAddressVerificationDao> {
}
