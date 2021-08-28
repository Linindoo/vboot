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
package cn.vtohru.orm.typehandler.stringbased.handlers;

import cn.vtohru.orm.dataaccess.query.impl.GeoSearchArgument;
import cn.vtohru.orm.datatypes.geojson.GeoPoint;
import cn.vtohru.orm.datatypes.geojson.Position;
import cn.vtohru.orm.mapping.IProperty;
import cn.vtohru.orm.typehandler.AbstractTypeHandler;
import cn.vtohru.orm.typehandler.ITypeHandler;
import cn.vtohru.orm.typehandler.ITypeHandlerFactory;
import cn.vtohru.orm.typehandler.ITypeHandlerResult;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * An implementation of {@link ITypeHandler} which handles intances of {@link GeoPoint}
 * 
 * @author Michael Remme
 * 
 */
public class GeoPointTypeHandler extends AbstractTypeHandler {

  /**
   * Comment for <code>COORDINATES</code>
   */
  private static final String COORDINATES = "coordinates";

  /**
   * @param typeHandlerFactory
   */
  public GeoPointTypeHandler(ITypeHandlerFactory typeHandlerFactory) {
    super(typeHandlerFactory, GeoPoint.class);
  }

  /*
   * (non-Javadoc)
   * 
   * @see cn.vtohru.orm.typehandler.ITypeHandler#fromStore(java.lang.Object,
   * cn.vtohru.orm.mapping.IField, java.lang.Class, io.vertx.core.Handler)
   */
  @Override
  public void fromStore(Object source, IProperty field, Class<?> cls,
      Handler<AsyncResult<ITypeHandlerResult>> resultHandler) {
    success(source == null ? source : parse(new JsonObject((String) source)), resultHandler);
  }

  /*
   * (non-Javadoc)
   * 
   * @see cn.vtohru.orm.typehandler.ITypeHandler#intoStore(java.lang.Object,
   * cn.vtohru.orm.mapping.IField, io.vertx.core.Handler)
   */
  @Override
  public void intoStore(Object source, IProperty field, Handler<AsyncResult<ITypeHandlerResult>> resultHandler) {
    if (source == null) {
      success(null, resultHandler);
    } else if (source instanceof GeoPoint) {
      success(encode((GeoPoint) source).toString(), resultHandler);
    } else if (source instanceof GeoSearchArgument) {
      success(encode((GeoSearchArgument) source).toString(), resultHandler);
    } else {
      fail(new UnsupportedOperationException("unsupported type: " + source.getClass().getName()), resultHandler);
    }
  }

  protected JsonObject encode(GeoSearchArgument source) {
    JsonObject ret = new JsonObject();
    ret.put("$geometry", encode((GeoPoint) source.getGeoJson()));
    if (source.getMaxDistance() >= 0) {
      ret.put("$maxDistance", source.getMaxDistance());
    }
    return ret;
  }

  protected JsonObject encode(GeoPoint source) {
    JsonObject result = new JsonObject();
    result.put("type", (source).getType().getTypeName()).put(COORDINATES,
        new JsonArray(source.getCoordinates().getValues()));
    return result;
  }

  protected GeoPoint parse(JsonObject source) {
    Position pos = new Position(source.getJsonArray(COORDINATES).iterator());
    return new GeoPoint(pos);
  }

}
