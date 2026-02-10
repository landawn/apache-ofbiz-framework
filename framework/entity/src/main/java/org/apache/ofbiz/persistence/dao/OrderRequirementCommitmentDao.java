package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.OrderRequirementCommitmentEntity;

public interface OrderRequirementCommitmentDao extends
        CrudDao<OrderRequirementCommitmentEntity, OrderRequirementCommitmentEntity, SQLBuilder.PSC, OrderRequirementCommitmentDao>,
        DelegatorQueryDao {
}
