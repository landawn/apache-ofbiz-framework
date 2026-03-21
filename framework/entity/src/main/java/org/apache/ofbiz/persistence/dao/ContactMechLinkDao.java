package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ContactMechLinkEntity;

public interface ContactMechLinkDao extends CrudDao<ContactMechLinkEntity, ContactMechLinkEntity, SqlBuilder.PSC, ContactMechLinkDao> {
}
