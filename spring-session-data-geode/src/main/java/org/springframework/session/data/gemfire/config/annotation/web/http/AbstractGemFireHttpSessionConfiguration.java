/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.session.data.gemfire.config.annotation.web.http;

import static org.springframework.data.gemfire.util.RuntimeExceptionFactory.newIllegalStateException;

import java.util.Objects;
import java.util.Optional;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.session.config.annotation.web.http.SpringHttpSessionConfiguration;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * The {@link AbstractGemFireHttpSessionConfiguration} class is an abstract base class containing configuration logic
 * common to Apache Geode and Pivotal GemFire in order to manage {@link javax.servlet.http.HttpSession} state.
 *
 * @author John Blum
 * @see java.lang.ClassLoader
 * @see org.springframework.beans.factory.BeanClassLoaderAware
 * @see org.springframework.beans.factory.config.ConfigurableBeanFactory
 * @see org.springframework.core.env.Environment
 * @see org.springframework.context.ApplicationContext
 * @see org.springframework.context.ConfigurableApplicationContext
 * @see org.springframework.context.EnvironmentAware
 * @see org.springframework.session.config.annotation.web.http.SpringHttpSessionConfiguration
 * @since 2.0.4
 */
@SuppressWarnings("unused")
public abstract class AbstractGemFireHttpSessionConfiguration extends SpringHttpSessionConfiguration
		implements BeanClassLoaderAware, EnvironmentAware {

	protected static final String SPRING_SESSION_PROPERTY_PREFIX = "spring.session.data.gemfire.";

	private ApplicationContext applicationContext;

	private ClassLoader beanClassLoader;

	private Environment environment;

	/**
	 * Sets a reference the Spring {@link ApplicationContext}.
	 *
	 * @param applicationContext reference to the Spring {@link ApplicationContext}.
	 * @throws BeansException if the reference cannot be stored.
	 * @see org.springframework.context.ApplicationContext
	 */
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		super.setApplicationContext(applicationContext);
		this.applicationContext = applicationContext;
	}

	/**
	 * Returns a reference to the Spring {@link ApplicationContext}.
	 *
	 * @return a reference to the Spring {@link ApplicationContext}.
	 * @see org.springframework.context.ApplicationContext
	 */
	protected ApplicationContext getApplicationContext() {
		return Optional.ofNullable(this.applicationContext)
			.orElseThrow(() -> newIllegalStateException("The ApplicationContext was not properly configured"));
	}

	/**
	 * Sets a reference to the {@link ClassLoader} used by the Spring container to load bean {@link Class class types}.
	 *
	 * @param beanClassLoader {@link ClassLoader} used by the Spring container to load bean {@link Class class types}.
	 * @see org.springframework.beans.factory.BeanClassLoaderAware#setBeanClassLoader(ClassLoader)
	 * @see java.lang.ClassLoader
	 */
	public void setBeanClassLoader(ClassLoader beanClassLoader) {
		this.beanClassLoader = beanClassLoader;
	}

	/**
	 * Returns a reference to the {@link ClassLoader} used by the Spring container to load bean
	 * {@link Class class types}.
	 *
	 * @return the {@link ClassLoader} used by the Spring container to load bean {@link Class class types}.
	 * @see java.lang.ClassLoader
	 */
	protected ClassLoader getBeanClassLoader() {
		return this.beanClassLoader;
	}

	/**
	 * Returns a reference to the Spring container {@link ConfigurableBeanFactory}.
	 *
	 * @return a reference to the Spring container {@link ConfigurableBeanFactory}.
	 * @see org.springframework.beans.factory.config.ConfigurableBeanFactory
	 * @see #getApplicationContext()
	 */
	protected ConfigurableBeanFactory getBeanFactory() {

		ApplicationContext applicationContext = getApplicationContext();

		return Optional.ofNullable(applicationContext)
			.filter(it -> it instanceof ConfigurableApplicationContext)
			.map(it -> ((ConfigurableApplicationContext) it).getBeanFactory())
			.orElseThrow(() -> newIllegalStateException("Unable to resolve a reference to a [%1$s] from a [%2$s]",
				ConfigurableBeanFactory.class.getName(), ObjectUtils.nullSafeClassName(applicationContext)));
	}

	/**
	 * Sets a reference to the Spring {@link Environment}.
	 *
	 * @param environment Spring {@link Environment}.
	 * @see org.springframework.core.env.Environment
	 */
	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	/**
	 * Returns a reference to the configured Spring {@link Environment}.
	 *
	 * @return a reference to the configured Spring {@link Environment}.
	 * @see org.springframework.core.env.Environment
	 */
	protected Environment getEnvironment() {
		return this.environment;
	}

	protected String clientRegionShortcutPropertyName() {
		return propertyName("cache.client.region.shortcut");
	}

	protected String indexableSessionAttributesPropertyName() {
		return sessionPropertyName("attributes.indexable");
	}

	protected String maxInactiveIntervalInSecondsPropertyName() {
		return sessionPropertyName("expiration.max-inactive-interval-seconds");
	}

	protected String poolNamePropertyName() {
		return propertyName("cache.client.pool.name");
	}

	protected String serverRegionShortcutPropertyName() {
		return propertyName("cache.server.region.shortcut");
	}

	protected String sessionPropertyName(String propertyNameSuffix) {
		return propertyName(String.format("session.%s", propertyNameSuffix));
	}

	protected String sessionRegionNamePropertyName() {
		return sessionPropertyName("region.name");
	}

	protected String sessionSerializerBeanNamePropertyName() {
		return sessionPropertyName("serializer.bean-name");
	}

	/**
	 * Returns the fully-qualified {@link String property name}.
	 *
	 * The fully qualified {@link String property name} consists of the {@link String base property name}
	 * concatenated with the {@code propertyNameSuffix}.
	 *
	 * @param propertyNameSuffix {@link String} containing the property name suffix concatenated with
	 * the {@link String base property name}.
	 * @return the fully-qualified {@link String property name}.
	 * @see java.lang.String
	 */
	protected String propertyName(String propertyNameSuffix) {
		return String.format("%1$s%2$s", SPRING_SESSION_PROPERTY_PREFIX, propertyNameSuffix);
	}

	/**
	 * Resolves the value for the given property identified by {@link String name} from the Spring {@link Environment}
	 * as an instance of the specified {@link Class type}.
	 *
	 * @param <T> {@link Class} type of the {@code propertyName property's} assigned value.
	 * @param propertyName {@link String} containing the name of the required property to resolve.
	 * @param type {@link Class} type of the property's assigned value.
	 * @return the assigned value of the {@link String named} property.
	 * @throws IllegalStateException if the property has not been assigned a value.
	 * For {@link String} values, this also means the value cannot be {@link String#isEmpty() empty}.
	 * For non-{@link String} values, this means the value must not be {@literal null}.
	 * @see #resolveProperty(String, Class, Object)
	 */
	protected <T> T requireProperty(String propertyName, Class<T> type) {

		return Optional.of(propertyName)
			.map(it -> resolveProperty(propertyName, type, null))
			.filter(Objects::nonNull)
			.filter(value -> !(value instanceof String) || StringUtils.hasText((String) value))
			.orElseThrow(() -> newIllegalStateException("Property [%s] is required", propertyName));
	}

	/**
	 * Attempts to resolve the property with the given {@link String name} from the Spring {@link Environment}
	 * as an {@link Enum}.
	 *
	 * @param propertyName {@link String name} of the property to resolve.
	 * @param defaultValue default value to return if the property is not defined or not set.
	 * @return the value of the property identified by {@link String name} or default value if the property
	 * is not defined or not set.
	 * @see #resolveProperty(String, Class, Object)
	 * @see java.lang.Enum
	 */
	protected <T extends Enum<T>> T resolveEnumeratedProperty(String propertyName, Class<T> targetType, T defaultValue) {

		return resolveProperty(propertyName, targetType, defaultValue);
	}

	/**
	 * Attempts to resolve the property with the given {@link String name} from the Spring {@link Environment}
	 * as an {@link Integer}.
	 *
	 * @param propertyName {@link String name} of the property to resolve.
	 * @param defaultValue default value to return if the property is not defined or not set.
	 * @return the value of the property identified by {@link String name} or default value if the property
	 * is not defined or not set.
	 * @see #resolveProperty(String, Class, Object)
	 * @see java.lang.Integer
	 */
	protected Integer resolveProperty(String propertyName, Integer defaultValue) {
		return resolveProperty(propertyName, Integer.class, defaultValue);
	}

	/**
	 * Attempts to resolve the property with the given {@link String name} from the Spring {@link Environment}
	 * as a {@link String}.
	 *
	 * @param propertyName {@link String name} of the property to resolve.
	 * @param defaultValue default value to return if the property is not defined or not set.
	 * @return the value of the property identified by {@link String name} or default value if the property
	 * is not defined or not set.
	 * @see #resolveProperty(String, Class, Object)
	 * @see java.lang.String
	 */
	protected String resolveProperty(String propertyName, String defaultValue) {
		return resolveProperty(propertyName, String.class, defaultValue);
	}

	/**
	 * Attempts to resolve the property with the given {@link String name} from the Spring {@link Environment}
	 * as a {@link String} array.
	 *
	 * @param propertyName {@link String name} of the property to resolve.
	 * @param defaultValue default value to return if the property is not defined or not set.
	 * @return the value of the property identified by {@link String name} or default value if the property
	 * is not defined or not set.
	 * @see #resolveProperty(String, Class, Object)
	 * @see java.lang.String
	 */
	protected String[] resolveProperty(String propertyName, String[] defaultValue) {
		return resolveProperty(propertyName, String[].class, defaultValue);
	}

	/**
	 * Attempts to resolve the property with the given {@link String name} from the Spring {@link Environment}.
	 *
	 * @param <T> {@link Class} type of the property value.
	 * @param propertyName {@link String name} of the property to resolve.
	 * @param targetType {@link Class} type of the property's value.
	 * @return the value of the property identified by {@link String name} or {@literal null} if the property
	 * is not defined or not set.
	 * @see #resolveProperty(String, Class, Object)
	 */
	protected <T> T resolveProperty(String propertyName, Class<T> targetType) {
		return resolveProperty(propertyName, targetType, null);
	}

	/**
	 * Attempts to resolve the property with the given {@link String name} from the Spring {@link Environment}.
	 *
	 * @param <T> {@link Class} type of the property value.
	 * @param propertyName {@link String name} of the property to resolve.
	 * @param targetType {@link Class} type of the property's value.
	 * @param defaultValue default value to return if the property is not defined or not set.
	 * @return the value of the property identified by {@link String name} or default value if the property
	 * is not defined or not set.
	 * @see #getEnvironment()
	 */
	protected <T> T resolveProperty(String propertyName, Class<T> targetType, T defaultValue) {

		return Optional.ofNullable(getEnvironment())
			.filter(environment -> environment.containsProperty(propertyName))
			.map(environment -> {

				String resolvedPropertyName = environment.resolveRequiredPlaceholders(propertyName);

				return environment.getProperty(resolvedPropertyName, targetType, defaultValue);
			})
			.orElse(defaultValue);
	}
}
