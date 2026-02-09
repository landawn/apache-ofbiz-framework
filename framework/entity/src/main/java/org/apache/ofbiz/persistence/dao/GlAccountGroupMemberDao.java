package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.GlAccountGroupMemberEntity;

public interface GlAccountGroupMemberDao extends CrudDao<GlAccountGroupMemberEntity, GlAccountGroupMemberEntity, SQLBuilder.PSC, GlAccountGroupMemberDao> {
}
