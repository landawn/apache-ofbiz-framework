package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import java.util.Collection;
import java.util.List;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.model.DynamicViewEntity;
import org.apache.ofbiz.entity.util.EntityFindOptions;
import org.apache.ofbiz.entity.util.EntityListIterator;
import org.apache.ofbiz.persistence.entity.PartyEntity;

public interface PartyDao extends CrudDao<PartyEntity, String, SqlBuilder.PSC, PartyDao>, DelegatorQueryDao {
    default EntityListIterator findIteratorByCondition(Delegator delegator, DynamicViewEntity dynamicViewEntity,
            EntityCondition whereCondition, Collection<String> fieldsToSelect, List<String> orderBy, EntityFindOptions findOptions)
            throws GenericEntityException {
        return delegator.findListIteratorByCondition(dynamicViewEntity, whereCondition, null, fieldsToSelect, orderBy, findOptions);
    }
}
