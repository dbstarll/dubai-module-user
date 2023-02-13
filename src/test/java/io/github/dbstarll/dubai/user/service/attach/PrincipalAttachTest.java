package io.github.dbstarll.dubai.user.service.attach;

import com.mongodb.client.model.Filters;
import io.github.dbstarll.dubai.model.entity.EntityFactory;
import io.github.dbstarll.dubai.model.service.ServiceFactory;
import io.github.dbstarll.dubai.model.service.ServiceTestCase;
import io.github.dbstarll.dubai.model.service.validate.DefaultValidate;
import io.github.dbstarll.dubai.model.service.validate.Validate;
import io.github.dbstarll.dubai.user.entity.AuthTypeEntity;
import io.github.dbstarll.dubai.user.entity.PrincipalEntity;
import io.github.dbstarll.dubai.user.entity.enums.AuthType;
import io.github.dbstarll.dubai.user.entity.join.PrincipalBase;
import io.github.dbstarll.dubai.user.service.AuthTypeService;
import io.github.dbstarll.dubai.user.service.PrincipalService;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map.Entry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrincipalAttachTest extends ServiceTestCase {
    private static final Class<PrincipalEntity> entityClass = PrincipalEntity.class;
    private static final Class<PrincipalService> serviceClass = PrincipalService.class;

    @BeforeAll
    static void beforeAll() {
        globalCollectionFactory();
    }

    @AfterAll
    static void afterAll() {
        cleanupGlobal();
    }

    @AfterEach
    void tearDown() {
        cleanupTest();
    }

    @Test
    void filterByPrincipalId() {
        final ObjectId principalId = new ObjectId();
        useService(serviceClass, s -> assertEquals(Filters.eq(PrincipalBase.FIELD_NAME_PRINCIPAL_ID, principalId),
                s.filterByPrincipalId(principalId)));
    }

    @Test
    void countByPrincipalId() {
        final ObjectId principalId = new ObjectId();
        useService(serviceClass, s -> {
            assertEquals(0, s.countByPrincipalId(principalId));
            final PrincipalEntity entity = EntityFactory.newInstance(entityClass);
            entity.setPrincipalId(principalId);
            assertSame(entity, s.save(entity, null));
            assertEquals(1, s.countByPrincipalId(principalId));
        });
    }

    @Test
    void findByPrincipalId() {
        final ObjectId principalId = new ObjectId();
        useService(serviceClass, s -> {
            assertNull(s.findByPrincipalId(principalId).first());
            final PrincipalEntity entity = EntityFactory.newInstance(entityClass);
            entity.setPrincipalId(principalId);
            assertSame(entity, s.save(entity, null));
            assertEquals(entity, s.findByPrincipalId(principalId).first());
        });
    }

    @Test
    void deleteByPrincipalId() {
        final ObjectId principalId = new ObjectId();
        useService(serviceClass, s -> {
            assertEquals(0, s.deleteByPrincipalId(principalId).getDeletedCount());
            final PrincipalEntity entity = EntityFactory.newInstance(entityClass);
            entity.setPrincipalId(principalId);
            assertSame(entity, s.save(entity, null));
            assertEquals(1, s.deleteByPrincipalId(principalId).getDeletedCount());
            assertNull(s.findById(entity.getId()));
        });
    }

    @Test
    void findWithPrincipal() {
        useCollectionFactory(cf -> {
            final PrincipalService service = ServiceFactory.newInstance(serviceClass, cf.newInstance(entityClass));
            final AuthTypeService principalService = ServiceFactory.newInstance(AuthTypeService.class,
                    cf.newInstance(AuthTypeEntity.class));

            assertNull(service.findWithPrincipal(principalService, null).first());

            final AuthTypeEntity principal = EntityFactory.newInstance(AuthTypeEntity.class);
            principal.setSource(AuthType.MiniProgram);
            assertNotNull(principalService.save(principal, null));

            final PrincipalEntity entity = EntityFactory.newInstance(entityClass);
            entity.setPrincipalId(new ObjectId());
            assertSame(entity, service.save(entity, null));

            final Entry<PrincipalEntity, AuthTypeEntity> match = service.findWithPrincipal(principalService, Filters.eq(entity.getId())).first();
            assertNotNull(match);
            assertEquals(entity, match.getKey());
            assertNull(match.getValue());

            final PrincipalEntity entity2 = EntityFactory.newInstance(entityClass);
            entity2.setPrincipalId(principal.getId());
            assertSame(entity2, service.save(entity2, null));

            final Entry<PrincipalEntity, AuthTypeEntity> match2 = service.findWithPrincipal(principalService, Filters.eq(entity2.getId())).first();
            assertNotNull(match2);
            assertEquals(entity2, match2.getKey());
            assertEquals(principal, match2.getValue());
        });
    }


    @Test
    void principalIdValidationNotSet() {
        useService(serviceClass, s -> {
            final PrincipalEntity entity = EntityFactory.newInstance(entityClass);
            final Validate validate = new DefaultValidate();
            assertNull(s.save(entity, validate));
            assertTrue(validate.hasErrors());
            assertTrue(validate.hasFieldErrors());
            assertEquals(Collections.singletonList("主体未设置"), validate.getFieldErrors()
                    .get(PrincipalBase.FIELD_NAME_PRINCIPAL_ID));
        });
    }

    @Test
    void principalIdValidationChange() {
        useService(serviceClass, s -> {
            final PrincipalEntity entity = EntityFactory.newInstance(entityClass);
            entity.setPrincipalId(new ObjectId());
            assertSame(entity, s.save(entity, null));

            final Validate validate = new DefaultValidate();
            assertNull(s.save(entity, validate));
            assertFalse(validate.hasErrors());

            entity.setPrincipalId(new ObjectId());
            assertNull(s.save(entity, validate));
            assertTrue(validate.hasErrors());
            assertTrue(validate.hasFieldErrors());
            assertEquals(Collections.singletonList("主体不可更改"), validate.getFieldErrors()
                    .get(PrincipalBase.FIELD_NAME_PRINCIPAL_ID));
        });
    }
}