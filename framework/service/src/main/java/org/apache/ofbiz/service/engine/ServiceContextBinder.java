/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ofbiz.service.engine;

import java.beans.Introspector;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.ObjectType;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.service.GenericServiceException;

/**
 * Binds map-based service context values to typed service context classes.
 */
final class ServiceContextBinder {

    private ServiceContextBinder() {
    }

    static Object bindContext(Map<String, Object> context, Class<?> targetType, String serviceName) throws GenericServiceException {
        if (context == null || targetType == null) {
            return context;
        }
        if (targetType.isInstance(context)) {
            return context;
        }
        if (Map.class.isAssignableFrom(targetType)) {
            return copyToTypedMapIfNeeded(context, targetType, serviceName);
        }
        return bindMapToBean(context, targetType, serviceName);
    }

    private static Object copyToTypedMapIfNeeded(Map<String, Object> context, Class<?> targetType, String serviceName)
            throws GenericServiceException {
        if (targetType.isInterface() || Modifier.isAbstract(targetType.getModifiers())) {
            return context;
        }
        Object mapInstance = instantiateTargetType(targetType, serviceName);
        Map<String, Object> outMap = UtilGenerics.cast(mapInstance);
        outMap.putAll(context);
        return outMap;
    }

    private static Object bindMapToBean(Map<String, Object> context, Class<?> beanType, String serviceName)
            throws GenericServiceException {
        Object bean = instantiateTargetType(beanType, serviceName);
        Map<String, Method> setters = getSetters(beanType);

        for (Map.Entry<String, Object> entry : context.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (key == null) {
                continue;
            }

            Method setter = setters.get(key);
            if (setter != null) {
                setViaSetter(bean, setter, value, serviceName, key);
                continue;
            }

            Field field = getField(beanType, key);
            if (field != null && !Modifier.isStatic(field.getModifiers()) && !Modifier.isFinal(field.getModifiers())) {
                setViaField(bean, field, value, serviceName, key);
            }
        }

        return bean;
    }

    private static void setViaSetter(Object bean, Method setter, Object rawValue, String serviceName, String fieldName)
            throws GenericServiceException {
        Class<?> parameterType = setter.getParameterTypes()[0];
        Object convertedValue = convertValue(rawValue, parameterType, serviceName, fieldName);
        if (convertedValue == null && parameterType.isPrimitive()) {
            return;
        }
        try {
            setter.invoke(bean, convertedValue);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new GenericServiceException("Failed binding context field [" + fieldName + "] for service [" + serviceName + "]", e);
        }
    }

    private static void setViaField(Object bean, Field field, Object rawValue, String serviceName, String fieldName)
            throws GenericServiceException {
        Object convertedValue = convertValue(rawValue, field.getType(), serviceName, fieldName);
        if (convertedValue == null && field.getType().isPrimitive()) {
            return;
        }
        try {
            field.setAccessible(true);
            field.set(bean, convertedValue);
        } catch (IllegalAccessException e) {
            throw new GenericServiceException("Failed binding context field [" + fieldName + "] for service [" + serviceName + "]", e);
        }
    }

    private static Object convertValue(Object value, Class<?> targetType, String serviceName, String fieldName)
            throws GenericServiceException {
        if (value == null) {
            return null;
        }

        Class<?> boxedTargetType = boxIfPrimitive(targetType);
        if (boxedTargetType.isInstance(value)) {
            return value;
        }

        if (value instanceof GenericValue && isPersistenceEntityType(boxedTargetType)) {
            return genericValueToEntity((GenericValue) value, boxedTargetType, serviceName);
        }

        if (value instanceof Map<?, ?> && !Map.class.isAssignableFrom(boxedTargetType) && isBindablePojoType(boxedTargetType)) {
            Map<String, Object> nestedMap = UtilGenerics.cast(value);
            return bindMapToBean(nestedMap, boxedTargetType, serviceName);
        }

        if (boxedTargetType.isEnum() && value instanceof String) {
            @SuppressWarnings({ "unchecked", "rawtypes" })
            Object enumValue = Enum.valueOf((Class<? extends Enum>) boxedTargetType, (String) value);
            return enumValue;
        }

        try {
            Object convertedValue = ObjectType.simpleTypeOrObjectConvert(value, boxedTargetType.getName(), null, null, null, false);
            if (convertedValue == null || boxedTargetType.isInstance(convertedValue)) {
                return convertedValue;
            }
        } catch (GeneralException e) {
            throw new GenericServiceException("Failed converting context field [" + fieldName + "] for service [" + serviceName + "]", e);
        }

        return value;
    }

    private static Object genericValueToEntity(GenericValue genericValue, Class<?> entityType, String serviceName)
            throws GenericServiceException {
        Object entity = instantiateTargetType(entityType, serviceName);
        Map<String, Method> setters = getSetters(entityType);

        for (Map.Entry<String, Method> setterEntry : setters.entrySet()) {
            String fieldName = setterEntry.getKey();
            Method setter = setterEntry.getValue();
            Object fieldValue = genericValue.get(fieldName);
            if (fieldValue == null) {
                continue;
            }
            setViaSetter(entity, setter, fieldValue, serviceName, fieldName);
        }

        return entity;
    }

    private static Map<String, Method> getSetters(Class<?> type) {
        Map<String, Method> setters = new HashMap<>();
        for (Method method : type.getMethods()) {
            if (!method.getName().startsWith("set") || method.getParameterCount() != 1 || !Modifier.isPublic(method.getModifiers())) {
                continue;
            }
            String propertyName = Introspector.decapitalize(method.getName().substring(3));
            setters.put(propertyName, method);
        }
        return setters;
    }

    private static Field getField(Class<?> type, String fieldName) {
        Class<?> current = type;
        while (current != null && !Object.class.equals(current)) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private static Object instantiateTargetType(Class<?> targetType, String serviceName) throws GenericServiceException {
        try {
            return targetType.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new GenericServiceException(
                    "Unable to instantiate typed context class [" + targetType.getName() + "] for service [" + serviceName + "]", e);
        }
    }

    private static boolean isPersistenceEntityType(Class<?> type) {
        Package typePackage = type.getPackage();
        return typePackage != null && "org.apache.ofbiz.persistence.entity".equals(typePackage.getName());
    }

    private static boolean isBindablePojoType(Class<?> type) {
        Package typePackage = type.getPackage();
        return typePackage != null && typePackage.getName().startsWith("org.apache.ofbiz.");
    }

    private static Class<?> boxIfPrimitive(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        if (boolean.class.equals(type)) {
            return Boolean.class;
        }
        if (byte.class.equals(type)) {
            return Byte.class;
        }
        if (short.class.equals(type)) {
            return Short.class;
        }
        if (int.class.equals(type)) {
            return Integer.class;
        }
        if (long.class.equals(type)) {
            return Long.class;
        }
        if (float.class.equals(type)) {
            return Float.class;
        }
        if (double.class.equals(type)) {
            return Double.class;
        }
        if (char.class.equals(type)) {
            return Character.class;
        }
        return type;
    }
}

