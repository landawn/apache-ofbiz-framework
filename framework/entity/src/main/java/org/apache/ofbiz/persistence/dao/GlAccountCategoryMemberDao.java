package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.GlAccountCategoryMemberEntity;

public interface GlAccountCategoryMemberDao extends CrudDao<GlAccountCategoryMemberEntity, GlAccountCategoryMemberEntity, SqlBuilder.PSC, GlAccountCategoryMemberDao> {
}
