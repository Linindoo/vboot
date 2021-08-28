/*
 * #%L
 * vertx-pojo-mapper-common
 * %%
 * Copyright (C) 2017 Braintags GmbH
 * %%
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * #L%
 */
package cn.vtohru.orm.mapping.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import cn.vtohru.orm.annotation.lifecycle.AfterLoad;
import cn.vtohru.orm.mapping.IMapper;
import cn.vtohru.orm.mapping.IObjectReference;
import cn.vtohru.orm.mapping.IProperty;
import cn.vtohru.orm.mapping.IStoreObject;
import io.vertx.core.*;

/**
 * An abstract implementation of IStoreObject
 * 
 * @author Michael Remme
 * @param <T>
 *          the type of the entity
 * @param <F>
 *          the type, which is used internally as format, like Json for instance
 */
public abstract class AbstractStoreObject<T, F> implements IStoreObject<T, F> {
  private static final io.vertx.core.logging.Logger LOGGER = io.vertx.core.logging.LoggerFactory
      .getLogger(AbstractStoreObject.class);

  private final IMapper<T> mapper;
  private T entity = null;
  private final Collection<IObjectReference> objectReferences = new ArrayList<>();
  private boolean newInstance = true;
  protected F container;

  public AbstractStoreObject(final IMapper<T> mapper, final T entity, final F container) {
    if (mapper == null)
      throw new NullPointerException("Mapper must not be null");
    if (entity == null)
      throw new NullPointerException("Entity must not be null");
    this.mapper = mapper;
    this.entity = entity;
    this.container = container;
  }

  public AbstractStoreObject(final F container, final IMapper<T> mapper) {
    if (mapper == null)
      throw new NullPointerException("Mapper must not be null");
    if (container == null)
      throw new NullPointerException("Container must not be null");
    this.mapper = mapper;
    this.container = container;
  }

  /**
   * @return the newInstance
   */
  @Override
  public final boolean isNewInstance() {
    return newInstance;
  }

  /**
   * @param newInstance
   *          the newInstance to set
   */
  public final void setNewInstance(final boolean newInstance) {
    this.newInstance = newInstance;
  }

  /**
   * @return the mapper
   */
  public final IMapper<T> getMapper() {
    return mapper;
  }

  @Override
  public final T getEntity() {
    if (entity == null) {
      String message = String.format("Internal Entity is not initialized; call method %s.initToEntity first ",
          getClass().getName());
      throw new NullPointerException(message);
    }
    return entity;
  }

  @Override
  public final Collection<IObjectReference> getObjectReferences() {
    return objectReferences;
  }

  @Override
  public final F getContainer() {
    return container;
  }

  /**
   * Initialize the internal entity from the information previously read from the datastore.
   * 
   * @param handler
   */
  public void initToEntity(final Handler<AsyncResult<Void>> handler) {
    T tmpObject = getMapper().getObjectFactory().createInstance(getMapper().getMapperClass());
    LOGGER.debug("start initToEntity");
    iterateFields(tmpObject, fieldResult -> {
      if (fieldResult.failed()) {
        handler.handle(fieldResult);
        return;
      }
      iterateObjectReferences(tmpObject, orResult -> {
        if (orResult.failed()) {
          handler.handle(orResult);
          return;
        }
        finishToEntity(tmpObject, handler);
        LOGGER.debug("finished initToEntity");
      });
    });
  }

  protected void finishToEntity(final T tmpObject, final Handler<AsyncResult<Void>> handler) {
    this.entity = tmpObject;
    getMapper().executeLifecycle(AfterLoad.class, entity, handler);
  }

  @SuppressWarnings("rawtypes")
  protected final void iterateFields(final T tmpObject, final Handler<AsyncResult<Void>> handler) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("start iterateFields");
    }
    Set<String> fieldNames = getMapper().getFieldNames();
    List<Future> fl = new ArrayList<>(fieldNames.size());
    for (String fieldName : fieldNames) {
      Promise<Void> f = Promise.promise();
      fl.add(f.future());
      IProperty field = getMapper().getField(fieldName);
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("handling field " + field.getFullName());
      }
      field.getPropertyMapper().fromStoreObject(tmpObject, this, field, f);
    }
    CompositeFuture cf = CompositeFuture.all(fl);
    cf.onComplete(cfr -> {
      if (cfr.failed()) {
        handler.handle(Future.failedFuture(cfr.cause()));
      } else {
        handler.handle(Future.succeededFuture());
      }
    });
  }

  protected void iterateObjectReferences(final Object tmpObject, final Handler<AsyncResult<Void>> handler) {
    LOGGER.debug("start iterateObjectReferences");
    if (getObjectReferences().isEmpty()) {
      LOGGER.debug("nothing to do");
      handler.handle(Future.succeededFuture());
      return;
    }
    Collection<IObjectReference> refs = getObjectReferences();
    List<Future> fl = new ArrayList<>(refs.size());
    for (IObjectReference ref : refs) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("handling object reference " + ref.getField().getFullName());
      }
      Promise<Void> f = Promise.promise();
      fl.add(f.future());
      ref.getField().getPropertyMapper().fromObjectReference(tmpObject, ref, f);
    }
    CompositeFuture cf = CompositeFuture.all(fl);
    cf.onComplete(cfr -> {
      if (cfr.failed()) {
        handler.handle(Future.failedFuture(cfr.cause()));
      } else {
        handler.handle(Future.succeededFuture());
      }
    });
  }

  /**
   * Initialize the internal entity into the StoreObject
   * 
   * @param handler
   */
  @SuppressWarnings("rawtypes")
  public void initFromEntity(final Handler<AsyncResult<Void>> handler) {
    List<Future> fl = new ArrayList<>();
    for (String fieldName : mapper.getFieldNames()) {
      fl.add(initFieldFromEntity(fieldName));
    }
    CompositeFuture cf = CompositeFuture.all(fl);
    cf.onComplete(cfr -> {
      if (cfr.failed()) {
        handler.handle(Future.failedFuture(cfr.cause()));
      } else {
        handler.handle(Future.succeededFuture());
      }
    });
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  protected Future initFieldFromEntity(final String fieldName) {
    Promise f = Promise.promise();
    IProperty field = mapper.getField(fieldName);
    field.getPropertyMapper().intoStoreObject(entity, this, field, f);
    return f.future();
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return mapper.getTableInfo().getName() + ": " + container;
  }

}
