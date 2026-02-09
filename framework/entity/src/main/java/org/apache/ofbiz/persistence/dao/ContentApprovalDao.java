package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ContentApprovalEntity;

public interface ContentApprovalDao extends CrudDao<ContentApprovalEntity, String, SQLBuilder.PSC, ContentApprovalDao> {
}
