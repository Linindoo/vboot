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

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;

import cn.vtohru.orm.annotation.field.ConcreteClass;
import cn.vtohru.orm.annotation.field.Embedded;
import cn.vtohru.orm.annotation.field.Encoder;
import cn.vtohru.orm.annotation.field.Id;
import cn.vtohru.orm.annotation.field.Ignore;
import cn.vtohru.orm.annotation.field.Property;
import cn.vtohru.orm.annotation.field.Referenced;
import cn.vtohru.orm.impl.IEncoder;
import cn.vtohru.orm.mapping.IMapper;
import cn.vtohru.orm.mapping.IProperty;
import cn.vtohru.orm.mapping.datastore.IColumnInfo;

/**
 * 
 * 
 * @author Michael Remme
 * 
 */
public abstract class AbstractProperty implements IProperty {
  /**
   * Annotations which shall be checked for a field definition
   */
  protected static final List<Class<? extends Annotation>> FIELD_ANNOTATIONS = Arrays.asList(Id.class, Property.class,
      Referenced.class, Embedded.class, ConcreteClass.class, Encoder.class, Ignore.class);
  private IMapper<?> mapper;
  private IEncoder encoder;
  /**
   * If for the current field an Annotation {@link Embedded} or {@link Referenced} is defined, then it is stored in here
   */
  protected Annotation embedRef;

  /**
   * 
   */
  public AbstractProperty(IMapper<?> mapper) {
    this.mapper = mapper;
  }

  /**
   * Checks for the annotation {@link Encoder} and determines a suitable encoder
   */
  protected void computeEncoder() {
    if (hasAnnotation(Encoder.class)) {
      String encoderName = ((Encoder) getAnnotation(Encoder.class)).name();
      IEncoder enc = getMapper().getMapperFactory().getDataStore().getEncoder(encoderName);
      if (enc == null) {
        throw new UnsupportedOperationException("The encoder with name " + encoderName
            + " does not exist. You need to add it into the datastore. Field: " + getFullName());
      }
      if (CharSequence.class.isAssignableFrom(getType())) {
        this.encoder = enc;
      } else {
        throw new UnsupportedOperationException(
            "Annotation Encoded can only be used for fields instance of CharSequence. Field: " + getFullName());
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see cn.vtohru.orm.mapping.IField#getMapper()
   */
  @Override
  public final IMapper<?> getMapper() {
    return mapper;
  }

  /*
   * (non-Javadoc)
   * 
   * @see cn.vtohru.orm.mapping.IField#getColumnInfo()
   */
  @Override
  public IColumnInfo getColumnInfo() {
    return getMapper().getTableInfo().getColumnInfo(this);
  }

  /*
   * (non-Javadoc)
   * 
   * @see cn.vtohru.orm.mapping.IField#isIdField()
   */
  @Override
  public boolean isIdField() {
    return hasAnnotation(Id.class);
  }

  /**
   * @return the encoder
   */
  @Override
  public IEncoder getEncoder() {
    return encoder;
  }

  /**
   * @param encoder
   *          the encoder to set
   */
  public void setEncoder(IEncoder encoder) {
    this.encoder = encoder;
  }

  @Override
  public Annotation getEmbedRef() {
    return embedRef;
  }

  @Override
  public String toString() {
    return getFullName();
  }

}
