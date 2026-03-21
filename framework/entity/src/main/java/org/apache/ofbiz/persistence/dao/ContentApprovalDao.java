package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ContentApprovalEntity;

public interface ContentApprovalDao extends CrudDao<ContentApprovalEntity, String, SqlBuilder.PSC, ContentApprovalDao> {
}
