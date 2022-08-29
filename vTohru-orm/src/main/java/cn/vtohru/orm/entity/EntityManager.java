package cn.vtohru.orm.entity;

import cn.vtohru.orm.exception.OrmException;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.beans.BeanIntrospection;
import io.micronaut.core.beans.BeanProperty;
import io.vertx.core.json.JsonObject;

import javax.inject.Singleton;
import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class EntityManager {
    private Map<Class<?>, EntityInfo> entityInfoMap = new ConcurrentHashMap<>();


    public EntityInfo getEntity(Class<?> entityClass) {
        EntityInfo entityInfo = entityInfoMap.get(entityClass);
        if (entityInfo != null) {
            return entityInfo;
        }
       return createEntity(entityClass);
    }

    protected EntityInfo createEntity(Class<?> entityClass) {
        BeanIntrospection<?> introspection = BeanIntrospection.getIntrospection(entityClass);
        EntityInfo entityInfo = new EntityInfo();
        AnnotationValue<Entity> annotation = introspection.getAnnotation(Entity.class);
        if (annotation == null) {
            throw new OrmException("该class不是有效的table实体映射类");
        }
        String tableName =  annotation.stringValue("name").orElse(entityClass.getSimpleName());
        entityInfo.setTableName(tableName);
        entityInfo.setBeanIntrospection(introspection);
        Map<String, EntityField> fieldMap = entityInfo.getFieldMap();

        List<EntityField> keyFields = new ArrayList<>();
        for (BeanProperty<?, ?> fieldProperty : introspection.getBeanProperties()) {
            AnnotationValue<Column> columnAnnotationValue = fieldProperty.getAnnotation(Column.class);
            if (columnAnnotationValue != null) {
                String fieldName = columnAnnotationValue.stringValue("name").orElse(fieldProperty.getName());
                EntityField entityField = new EntityField();
                entityField.setFieldName(fieldName);
                entityField.setProperty(fieldProperty);
                fieldMap.put(fieldProperty.getName(), entityField);
                AnnotationValue<GeneratedValue> propertyAnnotation = fieldProperty.getAnnotation(GeneratedValue.class);
                if (propertyAnnotation != null) {
                    Optional<GenerationType> strategy = propertyAnnotation.get("strategy", GenerationType.class);
                    if (strategy.isPresent()) {
                        entityField.setGenerationType(strategy.get().name());
                    }
                }
                if (fieldProperty.hasAnnotation(Id.class)) {
                    entityField.setPrimary(true);
                    keyFields.add(entityField);
                } else {
                    entityField.setPrimary(false);
                }
            }
        }
        entityInfo.setKeyFields(keyFields);
        return entityInfo;
    }

    public <T> boolean existPrimary(T model) {
        EntityInfo entity = getEntity(model.getClass());
        for (Map.Entry<String, EntityField> entityFieldEntry : entity.getFieldMap().entrySet()) {
            EntityField entityField = entityFieldEntry.getValue();
            if (entityField.isPrimary() && entityField.getProperty().get(model) == null) {
                return false;
            }
        }
        return true;
    }

    public <T> T convertEntity(JsonObject row, Class<T> entityClass) {
        EntityInfo entity = getEntity(entityClass);
        T bean = (T) entity.getBeanIntrospection().instantiate();
        for (Map.Entry<String, EntityField> fieldEntry : entity.getFieldMap().entrySet()) {
            fieldEntry.getValue().getProperty().set(bean, row.getValue(fieldEntry.getValue().getFieldName()));
        }
        return bean;
    }
}
