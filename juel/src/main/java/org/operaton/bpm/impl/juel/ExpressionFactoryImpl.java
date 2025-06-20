/*
 * Based on JUEL 2.2.1 code, 2006-2009 Odysseus Software GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.impl.juel;

import jakarta.el.ELContext;
import jakarta.el.ELException;
import jakarta.el.ExpressionFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.EnumSet;
import java.util.Properties;

/**
 * Expression factory implementation.
 *
 * This class is also used as an EL "service provider". The <em>JUEL</em> jar file specifies this
 * class as el expression factory implementation in
 * <code>META-INF/services/jakarta.el.ExpressionFactory</code>. Calling
 * {@link ExpressionFactory#newInstance()} will then return an instance of this class, configured as
 * described below.
 *
 * If no properties are specified at construction time, properties are read from
 * <ol>
 * <li>
 * If the file <code>JAVA_HOME/lib/el.properties</code> exists and if it contains property
 * <code>jakarta.el.ExpressionFactory</code> whose value is the name of this class, these properties
 * are taken as default properties.</li>
 * <li>Otherwise, if system property <code>jakarta.el.ExpressionFactory</code> is set to the name of
 * this class, the system properties {@link System#getProperties()} are taken as default properties.
 * </li>
 * <li>
 * <code>el.properties</code> on your classpath. These properties override the properties from
 * <code>JAVA_HOME/lib/el.properties</code> or {@link System#getProperties()}.</li>
 * </ol>
 * There are also constructors to explicitly pass in an instance of {@link Properties}.
 *
 * Having this, the following properties are read:
 * <ul>
 * <li>
 * <code>jakarta.el.cacheSize</code> - cache size (int, default is 1000)</li>
 * <li>
 * <code>jakarta.el.methodInvocations</code> - allow method invocations as in
 * <code>${foo.bar(baz)}</code> (boolean, default is <code>false</code>).</li>
 * <li>
 * <code>jakarta.el.nullProperties</code> - resolve <code>null</code> properties as in
 * <code>${foo[null]}</code> (boolean, default is <code>false</code>).</li>
 * <li>
 * <code>jakarta.el.varArgs</code> - support function/method calls using varargs (boolean, default is
 * <code>false</code>).</li>
 * </ul>
 *
 * @author Christoph Beck
 */
public class ExpressionFactoryImpl extends jakarta.el.ExpressionFactory {
	/**
	 * A profile provides a default set of language features that will define the builder's
	 * behavior. A profile can be adjusted using the <code>jakarta.el.methodInvocations</code>,
	 * <code>jakarta.el.varArgs</code> and <code>jakarta.el.nullProperties</code> properties.
	 *
	 * @since 2.2
	 */
	public enum Profile {
		/**
		 * JEE5: none
		 */
		JEE5(EnumSet.noneOf(Builder.Feature.class)),
		/**
		 * JEE6: <code>jakarta.el.methodInvocations</code>, <code>jakarta.el.varArgs</code>. This is the
		 * default profile.
		 */
		JEE6(EnumSet.of(Builder.Feature.METHOD_INVOCATIONS, Builder.Feature.VARARGS));

		private final EnumSet<Builder.Feature> features;

		Profile(EnumSet<Builder.Feature> features) {
			this.features = features;
		}

		Builder.Feature[] features() {
			return features.toArray(new Builder.Feature[0]);
		}

		boolean contains(Builder.Feature feature) {
			return features.contains(feature);
		}
	}

	/**
	 * <code>jakarta.el.methodInvocations</code>
	 */
	public static final String PROP_METHOD_INVOCATIONS = "jakarta.el.methodInvocations";

	/**
	 * <code>jakarta.el.varArgs</code>
	 */
	public static final String PROP_VAR_ARGS = "jakarta.el.varArgs";

	/**
	 * <code>jakarta.el.nullProperties</code>
	 */
	public static final String PROP_NULL_PROPERTIES = "jakarta.el.nullProperties";

	/**
	 * <code>jakarta.el.cacheSize</code>
	 */
	public static final String PROP_CACHE_SIZE = "jakarta.el.cacheSize";

	private final TreeStore store;
	private final TypeConverter converter;

	/**
	 * Create a new expression factory using the default builder and cache implementations. The
	 * builder and cache are configured from <code>el.properties</code> (see above). The maximum
	 * cache size will be 1000 unless overridden in <code>el.properties</code>. The builder profile
	 * is {@link Profile#JEE6} (features may be overridden in <code>el.properties</code>).
	 */
	public ExpressionFactoryImpl() {
		this(Profile.JEE6);
	}

	/**
	 * Create a new expression factory using the default builder and cache implementations. The
	 * builder and cache are configured from the specified profile and <code>el.properties</code>
	 * (see above). The maximum cache size will be 1000 unless overridden in
	 * <code>el.properties</code>.
	 *
	 * @param profile
	 *            builder profile (features may be overridden in <code>el.properties</code>)
	 *
	 * @since 2.2
	 */
	public ExpressionFactoryImpl(Profile profile) {
		Properties properties = loadProperties("el.properties");
		this.store = createTreeStore(1000, profile, properties);
		this.converter = createTypeConverter(properties);
	}

	/**
	 * Create a new expression factory using the default builder and cache implementations. The
	 * builder and cache are configured using the specified properties. The maximum cache size will
	 * be 1000 unless overridden by property <code>jakarta.el.cacheSize</code>. The builder profile is
	 * {@link Profile#JEE6} (features may be overridden in <code>properties</code>).
	 *
	 * @param properties
	 *            used to initialize this factory (may be <code>null</code>)
	 */
	public ExpressionFactoryImpl(Properties properties) {
		this(Profile.JEE6, properties);
	}

	/**
	 * Create a new expression factory using the default builder and cache implementations. The
	 * builder and cache are configured using the specified profile and properties. The maximum
	 * cache size will be 1000 unless overridden by property <code>jakarta.el.cacheSize</code>.
	 *
	 * @param profile
	 *            builder profile (individual features may be overridden in properties)
	 * @param properties
	 *            used to initialize this factory (may be <code>null</code>)
	 *
	 * @since 2.2
	 */
	public ExpressionFactoryImpl(Profile profile, Properties properties) {
		this.store = createTreeStore(1000, profile, properties);
		this.converter = createTypeConverter(properties);
	}

	/**
	 * Create a new expression factory using the default builder and cache implementations. The
	 * builder and cache are configured using the specified properties. The maximum cache size will
	 * be 1000 unless overridden by property <code>jakarta.el.cacheSize</code>. The builder profile is
	 * {@link Profile#JEE6} (individual features may be overridden in <code>properties</code>).
	 *
	 * @param properties
	 *            used to initialize this factory (may be <code>null</code>)
	 * @param converter
	 *            custom type converter
	 */
	public ExpressionFactoryImpl(Properties properties, TypeConverter converter) {
		this(Profile.JEE6, properties, converter);
	}

	/**
	 * Create a new expression factory using the default builder and cache implementations. The
	 * builder and cache are configured using the specified profile and properties. The maximum
	 * cache size will be 1000 unless overridden by property <code>jakarta.el.cacheSize</code>.
	 *
	 * @param profile
	 *            builder profile (individual features may be overridden in properties)
	 * @param properties
	 *            used to initialize this factory (may be <code>null</code>)
	 * @param converter
	 *            custom type converter
	 *
	 * @since 2.2
	 */
	public ExpressionFactoryImpl(Profile profile, Properties properties, TypeConverter converter) {
		this.store = createTreeStore(1000, profile, properties);
		this.converter = converter;
	}

	/**
	 * Create a new expression factory.
	 *
	 * @param store
	 *            the tree store used to parse and cache parse trees.
	 */
	public ExpressionFactoryImpl(TreeStore store) {
		this(store, TypeConverter.DEFAULT);
	}

	/**
	 * Create a new expression factory.
	 *
	 * @param store
	 *            the tree store used to parse and cache parse trees.
	 * @param converter
	 *            custom type converter
	 */
	public ExpressionFactoryImpl(TreeStore store, TypeConverter converter) {
		this.store = store;
		this.converter = converter;
	}

	private Properties loadDefaultProperties() {
		String home = System.getProperty("java.home");
		String path = home + File.separator + "lib" + File.separator + "el.properties";
		File file = new File(path);
		if (file.exists()) {
			Properties properties = new Properties();
			try (InputStream input = new FileInputStream(file)) {
				properties.load(input);
			} catch (IOException e) {
				throw new ELException("Cannot read default EL properties", e);
			}
			if (getClass().getName().equals(properties.getProperty("jakarta.el.ExpressionFactory"))) {
				return properties;
			}
		}
		if (getClass().getName().equals(System.getProperty("jakarta.el.ExpressionFactory"))) {
			return System.getProperties();
		}
		return null;
	}

	private Properties loadProperties(String path) {
		Properties properties = new Properties(loadDefaultProperties());

		// try to find and load properties
		InputStream input = null;
		try {
			input = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
		} catch (SecurityException e) {
			input = ClassLoader.getSystemResourceAsStream(path);
		}
		if (input != null) {
			try {
				properties.load(input);
			} catch (IOException e) {
				throw new ELException("Cannot read EL properties", e);
			} finally {
				try {
					input.close();
				} catch (IOException e) {
					// ignore...
				}
			}
		}

		return properties;
	}

	private boolean getFeatureProperty(Profile profile, Properties properties, Builder.Feature feature, String property) {
		return Boolean.parseBoolean(properties.getProperty(property, String.valueOf(profile.contains(feature))));
	}

	/**
	 * Create the factory's tree store. This implementation creates a new tree store using the
	 * default builder and cache implementations. The builder and cache are configured using the
	 * specified properties. The maximum cache size will be as specified unless overridden by
	 * property <code>jakarta.el.cacheSize</code>.
	 */
	protected TreeStore createTreeStore(int defaultCacheSize, Profile profile, Properties properties) {
		// create builder
		TreeBuilder builder = null;
		if (properties == null) {
			builder = createTreeBuilder(null, profile.features());
		} else {
			EnumSet<Builder.Feature> features = EnumSet.noneOf(Builder.Feature.class);
			if (getFeatureProperty(profile, properties, Builder.Feature.METHOD_INVOCATIONS, PROP_METHOD_INVOCATIONS)) {
				features.add(Builder.Feature.METHOD_INVOCATIONS);
			}
			if (getFeatureProperty(profile, properties, Builder.Feature.VARARGS, PROP_VAR_ARGS)) {
				features.add(Builder.Feature.VARARGS);
			}
			if (getFeatureProperty(profile, properties, Builder.Feature.NULL_PROPERTIES, PROP_NULL_PROPERTIES)) {
				features.add(Builder.Feature.NULL_PROPERTIES);
			}
			builder = createTreeBuilder(properties, features.toArray(new Builder.Feature[0]));
		}

		// create cache
		int cacheSize = defaultCacheSize;
		if (properties != null && properties.containsKey(PROP_CACHE_SIZE)) {
			try {
				cacheSize = Integer.parseInt(properties.getProperty(PROP_CACHE_SIZE));
			} catch (NumberFormatException e) {
				throw new ELException("Cannot parse EL property " + PROP_CACHE_SIZE, e);
			}
		}
		Cache cache = cacheSize > 0 ? new Cache(cacheSize) : null;

		return new TreeStore(builder, cache);
	}

	/**
	 * Create the factory's type converter. This implementation takes the
	 * <code>de.odysseus.el.misc.TypeConverter</code> property as the name of a class implementing
	 * the <code>de.odysseus.el.misc.TypeConverter</code> interface. If the property is not set, the
	 * default converter (<code>TypeConverter.DEFAULT</code>) is used.
	 */
	protected TypeConverter createTypeConverter(Properties properties) {
		Class<?> clazz = load(TypeConverter.class, properties);
		if (clazz == null) {
			return TypeConverter.DEFAULT;
		}
		try {
			return TypeConverter.class.cast(clazz.getDeclaredConstructor().newInstance());
		} catch (Exception e) {
			throw new ELException("TypeConverter " + clazz + " could not be instantiated", e);
		}
	}

	/**
	 * Create the factory's builder. This implementation takes the
	 * <code>de.odysseus.el.tree.TreeBuilder</code> property as a name of a class implementing the
	 * <code>de.odysseus.el.tree.TreeBuilder</code> interface. If the property is not set, a plain
	 * <code>de.odysseus.el.tree.impl.Builder</code> is used. If the configured class is a subclass
	 * of <code>de.odysseus.el.tree.impl.Builder</code> and which provides a constructor taking an
	 * array of <code>Builder.Feature</code>, this constructor will be invoked. Otherwise, the
	 * default constructor will be used.
	 */
	protected TreeBuilder createTreeBuilder(Properties properties, Builder.Feature... features) {
		Class<?> clazz = load(TreeBuilder.class, properties);
		if (clazz == null) {
			return new Builder(features);
		}
		try {
			if (Builder.class.isAssignableFrom(clazz)) {
				Constructor<?> constructor = clazz.getConstructor(Builder.Feature[].class);
				if (constructor == null) {
					if (features == null || features.length == 0) {
						return TreeBuilder.class.cast(clazz.getDeclaredConstructor().newInstance());
					} else {
						throw new ELException("Builder " + clazz + " is missing constructor (can't pass features)");
					}
				} else {
					return TreeBuilder.class.cast(constructor.newInstance((Object) features));
				}
			} else {
				return TreeBuilder.class.cast(clazz.getDeclaredConstructor().newInstance());
			}
		} catch (Exception e) {
			throw new ELException("TreeBuilder " + clazz + " could not be instantiated", e);
		}
	}

	private Class<?> load(Class<?> clazz, Properties properties) {
		if (properties != null) {
			String className = properties.getProperty(clazz.getName());
			if (className != null) {
				ClassLoader loader;
				try {
					loader = Thread.currentThread().getContextClassLoader();
				} catch (Exception e) {
					throw new ELException("Could not get context class loader", e);
				}
				try {
					return loader == null ? Class.forName(className) : loader.loadClass(className);
				} catch (ClassNotFoundException e) {
					throw new ELException("Class " + className + " not found", e);
				} catch (Exception e) {
					throw new ELException("Class " + className + " could not be instantiated", e);
				}
			}
		}
		return null;
	}

	@Override
	public <T> T coerceToType(Object obj, Class<T> targetType) {
		return converter.convert(obj, targetType);
	}

	@Override
	public final ObjectValueExpression createValueExpression(Object instance, Class<?> expectedType) {
		return new ObjectValueExpression(converter, instance, expectedType);
	}

	@Override
	public final TreeValueExpression createValueExpression(ELContext context, String expression, Class<?> expectedType) {
		return new TreeValueExpression(store, context.getFunctionMapper(), context.getVariableMapper(), converter,
				expression, expectedType);
	}

	@Override
	public final TreeMethodExpression createMethodExpression(ELContext context, String expression,
			Class<?> expectedReturnType, Class<?>[] expectedParamTypes) {
		return new TreeMethodExpression(store, context.getFunctionMapper(), context.getVariableMapper(), converter,
				expression, expectedReturnType, expectedParamTypes);
	}
}
