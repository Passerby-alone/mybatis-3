/**
 *    Copyright 2009-2020 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.builder.annotation;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.CacheNamespace;
import org.apache.ibatis.annotations.CacheNamespaceRef;
import org.apache.ibatis.annotations.Case;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.DeleteProvider;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.InsertProvider;
import org.apache.ibatis.annotations.Lang;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Options.FlushCachePolicy;
import org.apache.ibatis.annotations.Property;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.ResultMap;
import org.apache.ibatis.annotations.ResultType;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.SelectKey;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.annotations.TypeDiscriminator;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.UpdateProvider;
import org.apache.ibatis.binding.BindingException;
import org.apache.ibatis.binding.MapperMethod.ParamMap;
import org.apache.ibatis.builder.BuilderException;
import org.apache.ibatis.builder.CacheRefResolver;
import org.apache.ibatis.builder.IncompleteElementException;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.executor.keygen.Jdbc3KeyGenerator;
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.executor.keygen.NoKeyGenerator;
import org.apache.ibatis.executor.keygen.SelectKeyGenerator;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.Discriminator;
import org.apache.ibatis.mapping.FetchType;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ResultFlag;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.mapping.ResultSetType;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.mapping.StatementType;
import org.apache.ibatis.parsing.PropertyParser;
import org.apache.ibatis.reflection.TypeParameterResolver;
import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.UnknownTypeHandler;

/**
 * @author Clinton Begin
 * @author Kazuki Shimizu
 */
public class MapperAnnotationBuilder {

  /**
   * sql操作注解集合
   * */
  private static final Set<Class<? extends Annotation>> SQL_ANNOTATION_TYPES = new HashSet<>();
  /**
   * sql 操作提供者注解集合
   * */
  private static final Set<Class<? extends Annotation>> SQL_PROVIDER_ANNOTATION_TYPES = new HashSet<>();

  private final Configuration configuration;
  private final MapperBuilderAssistant assistant;
  /**
   * Mapper 接口类
   * */
  private final Class<?> type;

  static {
    SQL_ANNOTATION_TYPES.add(Select.class);
    SQL_ANNOTATION_TYPES.add(Insert.class);
    SQL_ANNOTATION_TYPES.add(Update.class);
    SQL_ANNOTATION_TYPES.add(Delete.class);

    SQL_PROVIDER_ANNOTATION_TYPES.add(SelectProvider.class);
    SQL_PROVIDER_ANNOTATION_TYPES.add(InsertProvider.class);
    SQL_PROVIDER_ANNOTATION_TYPES.add(UpdateProvider.class);
    SQL_PROVIDER_ANNOTATION_TYPES.add(DeleteProvider.class);
  }

  public MapperAnnotationBuilder(Configuration configuration, Class<?> type) {
    // 构建 MapperAnnotationBuilder 对象
    String resource = type.getName().replace('.', '/') + ".java (best guess)";
    this.assistant = new MapperBuilderAssistant(configuration, resource);
    this.configuration = configuration;
    this.type = type;
  }

  /**
   * 1. 解析Mapper 接口
   * 2. 解析Mapper对应的Mapper xml文件资源
   * */
  public void parse() {
    String resource = type.toString();
    // 判断当前 Mapper 接口是否应加载过
    if (!configuration.isResourceLoaded(resource)) {
      // 加载对应的 XML Mapper
      loadXmlResource();
      // 添加 Mapper 到已加载的集合中
      configuration.addLoadedResource(resource);
      // 设置namespace属性
      assistant.setCurrentNamespace(type.getName());
      // 解析cache标签
      parseCache();
      // 解析cache-ref标签
      parseCacheRef();
      // 遍历每个方法 解析每个方法上的解析
      for (Method method : type.getMethods()) {
        if (!canHaveStatement(method)) {
          continue;
        }
        if (getSqlCommandType(method) == SqlCommandType.SELECT && method.getAnnotation(ResultMap.class) == null) {
          parseResultMap(method);
        }
        try {
          // 执行解析
          parseStatement(method);
        } catch (IncompleteElementException e) {
          // 如果解析失败，则放置在incompleteMethods集合中
          configuration.addIncompleteMethod(new MethodResolver(this, method));
        }
      }
    }
    // 解析待定的方法
    parsePendingMethods();
  }

  private boolean canHaveStatement(Method method) {
    // issue #237
    return !method.isBridge() && !method.isDefault();
  }

  private void parsePendingMethods() {
    Collection<MethodResolver> incompleteMethods = configuration.getIncompleteMethods();
    synchronized (incompleteMethods) {
      Iterator<MethodResolver> iter = incompleteMethods.iterator();
      while (iter.hasNext()) {
        try {
          iter.next().resolve();
          iter.remove();
        } catch (IncompleteElementException e) {
          // This method is still missing a resource
        }
      }
    }
  }

  private void loadXmlResource() {
    // 判断 Mapper Xml 是否已经被加载过 如果加载过，就不加载了
    if (!configuration.isResourceLoaded("namespace:" + type.getName())) {
      // 获得Mapper xml资源对象
      String xmlResource = type.getName().replace('.', '/') + ".xml";
      // #1347
      InputStream inputStream = type.getResourceAsStream("/" + xmlResource);
      if (inputStream == null) {
        // Search XML mapper that is not in the module but in the classpath.
        try {
          inputStream = Resources.getResourceAsStream(type.getClassLoader(), xmlResource);
        } catch (IOException e2) {
          // ignore, resource is not required
        }
      }
      // 创建XMLMapperBuilder对象进行解析
      if (inputStream != null) {
        XMLMapperBuilder xmlParser = new XMLMapperBuilder(inputStream, assistant.getConfiguration(), xmlResource, configuration.getSqlFragments(), type.getName());
        xmlParser.parse();
      }
    }
  }

  private void parseCache() {
    // 获得类上的@CacheNamespace注解
    CacheNamespace cacheDomain = type.getAnnotation(CacheNamespace.class);
    if (cacheDomain != null) {
      // 获得各种属性
      Integer size = cacheDomain.size() == 0 ? null : cacheDomain.size();
      Long flushInterval = cacheDomain.flushInterval() == 0 ? null : cacheDomain.flushInterval();
      // 获得properties对象
      Properties props = convertToProperties(cacheDomain.properties());
      // 创建cache对象
      assistant.useNewCache(cacheDomain.implementation(), cacheDomain.eviction(), flushInterval, size, cacheDomain.readWrite(), cacheDomain.blocking(), props);
    }
  }

  private Properties convertToProperties(Property[] properties) {
    if (properties.length == 0) {
      return null;
    }
    Properties props = new Properties();
    for (Property property : properties) {
      props.setProperty(property.name(),
          PropertyParser.parse(property.value(), configuration.getVariables()));
    }
    return props;
  }

  private void parseCacheRef() {
    // 解析 CacheNamespaceRef 注解
    CacheNamespaceRef cacheDomainRef = type.getAnnotation(CacheNamespaceRef.class);
    if (cacheDomainRef != null) {
      // 获得CacheNamespaceRef中的value类 具体指Mapper.class
      Class<?> refType = cacheDomainRef.value();
      String refName = cacheDomainRef.name();
      // 如果 refType 和 refName 都为空
      if (refType == void.class && refName.isEmpty()) {
        throw new BuilderException("Should be specified either value() or name() attribute in the @CacheNamespaceRef");
      }
      // 如果 refType 和 refName 都不为空 也抛出异常
      if (refType != void.class && !refName.isEmpty()) {
        throw new BuilderException("Cannot use both value() and name() attribute in the @CacheNamespaceRef");
      }
      // 获得最终的 namespace 属性
      String namespace = (refType != void.class) ? refType.getName() : refName;
      try {
        // 获得namespace指向的 Cache 对象
        assistant.useCacheRef(namespace);
      } catch (IncompleteElementException e) {
        configuration.addIncompleteCacheRef(new CacheRefResolver(assistant, namespace));
      }
    }
  }

  private String parseResultMap(Method method) {
    // 获得返回类型
    Class<?> returnType = getReturnType(method);
    // 获得Arg，Result，TypeDiscriminator注解
    Arg[] args = method.getAnnotationsByType(Arg.class);
    Result[] results = method.getAnnotationsByType(Result.class);
    TypeDiscriminator typeDiscriminator = method.getAnnotation(TypeDiscriminator.class);
    // 生成resultMapId
    String resultMapId = generateResultMapName(method);
    // 生成resultMap对象
    applyResultMap(resultMapId, returnType, args, results, typeDiscriminator);
    return resultMapId;
  }

  private String generateResultMapName(Method method) {
    // 如果有@Results注解 并且有设置Id属性，则直接返回id属性，格式为：`${type.name}.${Results.id}`
    Results results = method.getAnnotation(Results.class);
    if (results != null && !results.id().isEmpty()) {
      return type.getName() + "." + results.id();
    }
    // 没有的话，需要自动生成 suffix = 获得方法参数构成的签名 如果没有参数 则 -void
    StringBuilder suffix = new StringBuilder();
    for (Class<?> c : method.getParameterTypes()) {
      suffix.append("-");
      suffix.append(c.getSimpleName());
    }
    if (suffix.length() < 1) {
      suffix.append("-void");
    }
    // `${type.name}.${method.name}${suffix}`
    return type.getName() + "." + method.getName() + suffix;
  }

  /**
   * 创建ResultMap 对象
   * */
  private void applyResultMap(String resultMapId, Class<?> returnType, Arg[] args, Result[] results, TypeDiscriminator discriminator) {
    List<ResultMapping> resultMappings = new ArrayList<>();
    // 将@Arg[] 注解数组 解析成对应的 ResultMapping 对象 并添加到 resultMappings 中
    applyConstructorArgs(args, returnType, resultMappings);
    // 将@Result[] 注解数组，解析成对应的 ResultMapping 对象 并添加到 resultMappings 中
    applyResults(results, returnType, resultMappings);
    // 创建 Discriminator 对象
    Discriminator disc = applyDiscriminator(resultMapId, returnType, discriminator);
    // TODO add AutoMappingBehaviour
    assistant.addResultMap(resultMapId, returnType, null, disc, resultMappings, null);
    // 创建 Discriminator 的 ResultMap 对象
    createDiscriminatorResultMaps(resultMapId, returnType, discriminator);
  }

  private void createDiscriminatorResultMaps(String resultMapId, Class<?> resultType, TypeDiscriminator discriminator) {
    if (discriminator != null) {
      // 遍历 @Case 注解
      for (Case c : discriminator.cases()) {
        // 获得 @Case 注解的 ResultMap编号
        String caseResultMapId = resultMapId + "-" + c.value();
        List<ResultMapping> resultMappings = new ArrayList<>();
        // issue #136
        applyConstructorArgs(c.constructArgs(), resultType, resultMappings);
        applyResults(c.results(), resultType, resultMappings);
        // TODO add AutoMappingBehaviour
        assistant.addResultMap(caseResultMapId, c.type(), resultMapId, null, resultMappings, null);
      }
    }
  }

  private Discriminator applyDiscriminator(String resultMapId, Class<?> resultType, TypeDiscriminator discriminator) {
    if (discriminator != null) {
      // 获得各种属性
      String column = discriminator.column();
      Class<?> javaType = discriminator.javaType() == void.class ? String.class : discriminator.javaType();
      JdbcType jdbcType = discriminator.jdbcType() == JdbcType.UNDEFINED ? null : discriminator.jdbcType();
      @SuppressWarnings("unchecked")
      Class<? extends TypeHandler<?>> typeHandler = (Class<? extends TypeHandler<?>>)
              (discriminator.typeHandler() == UnknownTypeHandler.class ? null : discriminator.typeHandler());
      Case[] cases = discriminator.cases();
      Map<String, String> discriminatorMap = new HashMap<>();
      for (Case c : cases) {
        String value = c.value();
        String caseResultMapId = resultMapId + "-" + value;
        discriminatorMap.put(value, caseResultMapId);
      }
      return assistant.buildDiscriminator(resultType, column, javaType, jdbcType, typeHandler, discriminatorMap);
    }
    return null;
  }

  /**
   * 解析方法上的SQL操作相关的注解
   * */
  void parseStatement(Method method) {
    // 获得方法上的参数类型 多个的话 ParamMap
    Class<?> parameterTypeClass = getParameterType(method);
    // 获得 LanguageDriver 对象
    LanguageDriver languageDriver = getLanguageDriver(method);
    // 获得SqlSource 对象
    SqlSource sqlSource = getSqlSourceFromAnnotations(method, parameterTypeClass, languageDriver);
    if (sqlSource != null) {
      // 获得各种属性
      Options options = method.getAnnotation(Options.class);
      final String mappedStatementId = type.getName() + "." + method.getName();
      Integer fetchSize = null;
      Integer timeout = null;
      StatementType statementType = StatementType.PREPARED;
      ResultSetType resultSetType = configuration.getDefaultResultSetType();
      // 获得SqlCommandType类型
      SqlCommandType sqlCommandType = getSqlCommandType(method);
      boolean isSelect = sqlCommandType == SqlCommandType.SELECT;
      boolean flushCache = !isSelect;
      boolean useCache = isSelect;

      // 获得 KeyGenerator 对象
      KeyGenerator keyGenerator;
      String keyProperty = null;
      String keyColumn = null;
      if (SqlCommandType.INSERT.equals(sqlCommandType) || SqlCommandType.UPDATE.equals(sqlCommandType)) {
        // 如果SelectKey注解，则进行处理
        SelectKey selectKey = method.getAnnotation(SelectKey.class);
        if (selectKey != null) {
          keyGenerator = handleSelectKeyAnnotation(selectKey, mappedStatementId, getParameterType(method), languageDriver);
          keyProperty = selectKey.keyProperty();
        // 如果Options注解为空，则根据全局配置处理
        } else if (options == null) {
          // 如果自动生成useGenerateKey主键 自动生成则使用JDBC3KeyGenerator生成
          keyGenerator = configuration.isUseGeneratedKeys() ? Jdbc3KeyGenerator.INSTANCE : NoKeyGenerator.INSTANCE;
        } else {
          keyGenerator = options.useGeneratedKeys() ? Jdbc3KeyGenerator.INSTANCE : NoKeyGenerator.INSTANCE;
          keyProperty = options.keyProperty();
          keyColumn = options.keyColumn();
        }
      } else {
        keyGenerator = NoKeyGenerator.INSTANCE;
      }
      // 初始化各种属性
      if (options != null) {
        if (FlushCachePolicy.TRUE.equals(options.flushCache())) {
          flushCache = true;
        } else if (FlushCachePolicy.FALSE.equals(options.flushCache())) {
          flushCache = false;
        }
        useCache = options.useCache();
        fetchSize = options.fetchSize() > -1 || options.fetchSize() == Integer.MIN_VALUE ? options.fetchSize() : null; //issue #348
        timeout = options.timeout() > -1 ? options.timeout() : null;
        statementType = options.statementType();
        if (options.resultSetType() != ResultSetType.DEFAULT) {
          resultSetType = options.resultSetType();
        }
      }
      // 获得 resultMapId 编号字符串 <resultMap id=""> = ResultMapId
      String resultMapId = null;
      if (isSelect) {
        // 如果有 @ResultMap 注解 使用该注解为 resultMapId 注解
        ResultMap resultMapAnnotation = method.getAnnotation(ResultMap.class);
        if (resultMapAnnotation != null) {
          resultMapId = String.join(",", resultMapAnnotation.value());
        } else {
          resultMapId = generateResultMapName(method);
        }
      }
      // 构建 MappedStatement 对象
      assistant.addMappedStatement(
          mappedStatementId,
          sqlSource,
          statementType,
          sqlCommandType,
          fetchSize,
          timeout,
          // ParameterMapID
          null,
          parameterTypeClass,
          resultMapId,
          getReturnType(method),
          resultSetType,
          flushCache,
          useCache,
          // TODO gcode issue #577
          false,
          keyGenerator,
          keyProperty,
          keyColumn,
          // DatabaseID
          null,
          languageDriver,
          // ResultSets
          options != null ? nullOrEmpty(options.resultSets()) : null);
    }
  }

  private LanguageDriver getLanguageDriver(Method method) {
    // 解析 @lang注解 获得对应的类型
    Lang lang = method.getAnnotation(Lang.class);
    Class<? extends LanguageDriver> langClass = null;
    if (lang != null) {
      langClass = lang.value();
    }
    // 如果有langClass 则使用langClass 如果没有langClass 则会使用默认的LanguageDriver
    return configuration.getLanguageDriver(langClass);
  }

  private Class<?> getParameterType(Method method) {
    Class<?> parameterType = null;
    // 遍历参数类型数组
    Class<?>[] parameterTypes = method.getParameterTypes();
    for (Class<?> currentParameterType : parameterTypes) {
      // 排除RowBounds 和 ResultHandler 类型
      if (!RowBounds.class.isAssignableFrom(currentParameterType) && !ResultHandler.class.isAssignableFrom(currentParameterType)) {
        if (parameterType == null) {
          // 如果是单参数那就是currentParameterType
          parameterType = currentParameterType;
        } else {
          // 如果是多参数 则就是ParamMap
          parameterType = ParamMap.class;
        }
      }
    }
    return parameterType;
  }

  private Class<?> getReturnType(Method method) {
    // 获得方法的返回类型
    Class<?> returnType = method.getReturnType();
    // 解析成对应的 Type
    Type resolvedReturnType = TypeParameterResolver.resolveReturnType(method, type);
    // 如果 resolvedReturnType 是 Class 说明是普通的类
    if (resolvedReturnType instanceof Class) {
      returnType = (Class<?>) resolvedReturnType;
      // 如果 returnType 是数组 则使用 componentType
      if (returnType.isArray()) {
        returnType = returnType.getComponentType();
      }
      // 如果是void 类型 则尝试使用ResultType
      if (void.class.equals(returnType)) {
        ResultType rt = method.getAnnotation(ResultType.class);
        if (rt != null) {
          returnType = rt.value();
        }
      }
      // 如果 resolvedReturnType 是 ParameterizedType 说明是范型
    } else if (resolvedReturnType instanceof ParameterizedType) {
      // 获得范型 rawType
      ParameterizedType parameterizedType = (ParameterizedType) resolvedReturnType;
      Class<?> rawType = (Class<?>) parameterizedType.getRawType();

      // 如果是 Collection 或者 Cursor 类型时
      if (Collection.class.isAssignableFrom(rawType) || Cursor.class.isAssignableFrom(rawType)) {

        // 获得 <> 中的实际类型
        Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
        // 如果 actualTypeArguments == 1
        if (actualTypeArguments != null && actualTypeArguments.length == 1) {
          Type returnTypeParameter = actualTypeArguments[0];
          if (returnTypeParameter instanceof Class<?>) {
            returnType = (Class<?>) returnTypeParameter;
          } else if (returnTypeParameter instanceof ParameterizedType) {
            // (gcode issue #443) actual type can be a also a parameterized type
            returnType = (Class<?>) ((ParameterizedType) returnTypeParameter).getRawType();
            // 如果是泛型数组类型，则获得 genericComponentType 对应的类
          } else if (returnTypeParameter instanceof GenericArrayType) {
            Class<?> componentType = (Class<?>) ((GenericArrayType) returnTypeParameter).getGenericComponentType();
            // (gcode issue #525) support List<byte[]>
            returnType = Array.newInstance(componentType, 0).getClass();
          }
        }
        // 如果包含MapKey 注解，并且返回类型是Map 类型
      } else if (method.isAnnotationPresent(MapKey.class) && Map.class.isAssignableFrom(rawType)) {
        // 获得<>中实际类型
        Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
        // 如果 actualTypeArguments 的大小为2，则进一步处理，Map -> key, value两个范型
        if (actualTypeArguments != null && actualTypeArguments.length == 2) {
          // 处理Map 的 V范型
          Type returnTypeParameter = actualTypeArguments[1];
          if (returnTypeParameter instanceof Class<?>) {
            returnType = (Class<?>) returnTypeParameter;
            // 如果 V 泛型为 ParameterizedType ，则获取 <> 中实际类型
          } else if (returnTypeParameter instanceof ParameterizedType) {
            // (gcode issue 443) actual type can be a also a parameterized type
            returnType = (Class<?>) ((ParameterizedType) returnTypeParameter).getRawType();
          }
        }
      } else if (Optional.class.equals(rawType)) {
        // 则获取Optional<T> 中实际类型
        Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
        Type returnTypeParameter = actualTypeArguments[0];
        if (returnTypeParameter instanceof Class<?>) {
          returnType = (Class<?>) returnTypeParameter;
        }
      }
    }

    return returnType;
  }

  private SqlSource getSqlSourceFromAnnotations(Method method, Class<?> parameterType, LanguageDriver languageDriver) {
    try {
      // 获得注解上的 SQL_ANNOTATION_TYPES 和 SQL_PROVIDER_ANNOTATION_TYPES 注解
      Class<? extends Annotation> sqlAnnotationType = getSqlAnnotationType(method);
      Class<? extends Annotation> sqlProviderAnnotationType = getSqlProviderAnnotationType(method);
      if (sqlAnnotationType != null) {
        if (sqlProviderAnnotationType != null) {
          throw new BindingException("You cannot supply both a static SQL and SqlProvider to method named " + method.getName());
        }
        // 获得 SQL_ANNOTATION_TYPES 对应的注解
        Annotation sqlAnnotation = method.getAnnotation(sqlAnnotationType);
        // 获得 value 属性
        final String[] strings = (String[]) sqlAnnotation.getClass().getMethod("value").invoke(sqlAnnotation);
        // 创建SqlSource对象
        return buildSqlSourceFromStrings(strings, parameterType, languageDriver);
      } else if (sqlProviderAnnotationType != null) {
        // 获得 SQL_PROVIDER_ANNOTATION_TYPES 对应的注解
        Annotation sqlProviderAnnotation = method.getAnnotation(sqlProviderAnnotationType);
        // 创建 ProviderSqlSource 对象
        return new ProviderSqlSource(assistant.getConfiguration(), sqlProviderAnnotation, type, method);
      }
      return null;
    } catch (Exception e) {
      throw new BuilderException("Could not find value method on SQL annotation.  Cause: " + e, e);
    }
  }

  private SqlSource buildSqlSourceFromStrings(String[] strings, Class<?> parameterTypeClass, LanguageDriver languageDriver) {
    // 拼接Sql
    return languageDriver.createSqlSource(configuration, String.join(" ", strings).trim(), parameterTypeClass);
  }

  private SqlCommandType getSqlCommandType(Method method) {
    // 获得符合 SQL_ANNOTATION_TYPES 类型的注解类型
    Class<? extends Annotation> type = getSqlAnnotationType(method);

    if (type == null) {
      // 获得 SQL_PROVIDER_ANNOTATION_TYPES 类型的注解类型
      type = getSqlProviderAnnotationType(method);

      if (type == null) {
        return SqlCommandType.UNKNOWN;
      }
      // provider类型的转换为不带provider类型的
      if (type == SelectProvider.class) {
        type = Select.class;
      } else if (type == InsertProvider.class) {
        type = Insert.class;
      } else if (type == UpdateProvider.class) {
        type = Update.class;
      } else if (type == DeleteProvider.class) {
        type = Delete.class;
      }
    }
    // 转换为对应的SqlCommandType
    return SqlCommandType.valueOf(type.getSimpleName().toUpperCase(Locale.ENGLISH));
  }

  private Class<? extends Annotation> getSqlAnnotationType(Method method) {
    return chooseAnnotationType(method, SQL_ANNOTATION_TYPES);
  }

  private Class<? extends Annotation> getSqlProviderAnnotationType(Method method) {
    return chooseAnnotationType(method, SQL_PROVIDER_ANNOTATION_TYPES);
  }

  /**
   * 找到符合指定类型的注解类型
   * */
  private Class<? extends Annotation> chooseAnnotationType(Method method, Set<Class<? extends Annotation>> types) {
    for (Class<? extends Annotation> type : types) {
      Annotation annotation = method.getAnnotation(type);
      if (annotation != null) {
        return type;
      }
    }
    return null;
  }

  /**
   * 解析@Result[] 注解数组
   * */
  private void applyResults(Result[] results, Class<?> resultType, List<ResultMapping> resultMappings) {
    // 遍历 @Result 注解
    for (Result result : results) {
      // 创建 ResultFlag 数组
      List<ResultFlag> flags = new ArrayList<>();
      if (result.id()) {
        flags.add(ResultFlag.ID);
      }
      @SuppressWarnings("unchecked")
        // 获得 TypeHandler 类
      Class<? extends TypeHandler<?>> typeHandler = (Class<? extends TypeHandler<?>>)
              ((result.typeHandler() == UnknownTypeHandler.class) ? null : result.typeHandler());
      // 判断是否内嵌的查询
      boolean hasNestedResultMap = hasNestedResultMap(result);
      // 构建 ResultMapping 对象
      ResultMapping resultMapping = assistant.buildResultMapping(
          resultType,
          nullOrEmpty(result.property()),
          nullOrEmpty(result.column()),
          result.javaType() == void.class ? null : result.javaType(),
          result.jdbcType() == JdbcType.UNDEFINED ? null : result.jdbcType(),
          hasNestedSelect(result) ? nestedSelectId(result) : null,
          hasNestedResultMap ? nestedResultMapId(result) : null,
          null,
          hasNestedResultMap ? findColumnPrefix(result) : null,
          typeHandler,
          flags,
          null,
          null,
          isLazy(result));
      resultMappings.add(resultMapping);
    }
  }

  private String findColumnPrefix(Result result) {
    String columnPrefix = result.one().columnPrefix();
    if (columnPrefix.length() < 1) {
      columnPrefix = result.many().columnPrefix();
    }
    return columnPrefix;
  }

  private String nestedResultMapId(Result result) {
    String resultMapId = result.one().resultMap();
    if (resultMapId.length() < 1) {
      resultMapId = result.many().resultMap();
    }
    if (!resultMapId.contains(".")) {
      resultMapId = type.getName() + "." + resultMapId;
    }
    return resultMapId;
  }

  private boolean hasNestedResultMap(Result result) {
    if (result.one().resultMap().length() > 0 && result.many().resultMap().length() > 0) {
      throw new BuilderException("Cannot use both @One and @Many annotations in the same @Result");
    }
    // 判断是否有one 和 many 注解
    return result.one().resultMap().length() > 0 || result.many().resultMap().length() > 0;
  }

  private String nestedSelectId(Result result) {
    // 获得 @One 注解
    String nestedSelect = result.one().select();
    if (nestedSelect.length() < 1) {
      // 如果 没有 @One 则获取 @Many注解
      nestedSelect = result.many().select();
    }
    // 获得内嵌查询编号，格式为 `{type.name}.${select}`
    if (!nestedSelect.contains(".")) {
      nestedSelect = type.getName() + "." + nestedSelect;
    }
    return nestedSelect;
  }

  /**
   * 判断是否懒加载
   * */
  private boolean isLazy(Result result) {
    // 判断 configuration 是否懒加载
    boolean isLazy = configuration.isLazyLoadingEnabled();
    // 判断是否有@One注解，则判断是否是懒加载
    if (result.one().select().length() > 0 && FetchType.DEFAULT != result.one().fetchType()) {
      isLazy = result.one().fetchType() == FetchType.LAZY;
      // 如果有@Many注解 则判断是否懒加载
    } else if (result.many().select().length() > 0 && FetchType.DEFAULT != result.many().fetchType()) {
      isLazy = result.many().fetchType() == FetchType.LAZY;
    }
    return isLazy;
  }

  private boolean hasNestedSelect(Result result) {
    if (result.one().select().length() > 0 && result.many().select().length() > 0) {
      throw new BuilderException("Cannot use both @One and @Many annotations in the same @Result");
    }
    return result.one().select().length() > 0 || result.many().select().length() > 0;
  }

  private void applyConstructorArgs(Arg[] args, Class<?> resultType, List<ResultMapping> resultMappings) {
    // 遍历 @Arg[] 数组
    for (Arg arg : args) {
      // 创建 ResultFlag 数组
      List<ResultFlag> flags = new ArrayList<>();
      flags.add(ResultFlag.CONSTRUCTOR);
      if (arg.id()) {
        flags.add(ResultFlag.ID);
      }
      @SuppressWarnings("unchecked")
        // 获得 TypeHandler 类型
      Class<? extends TypeHandler<?>> typeHandler = (Class<? extends TypeHandler<?>>)
              (arg.typeHandler() == UnknownTypeHandler.class ? null : arg.typeHandler());
      // 将当前 @Arg 注解构建成 ResultMapping 对象
      ResultMapping resultMapping = assistant.buildResultMapping(
          resultType,
          nullOrEmpty(arg.name()),
          nullOrEmpty(arg.column()),
          arg.javaType() == void.class ? null : arg.javaType(),
          arg.jdbcType() == JdbcType.UNDEFINED ? null : arg.jdbcType(),
          nullOrEmpty(arg.select()),
          nullOrEmpty(arg.resultMap()),
          null,
          nullOrEmpty(arg.columnPrefix()),
          typeHandler,
          flags,
          null,
          null,
          false);
      resultMappings.add(resultMapping);
    }
  }

  private String nullOrEmpty(String value) {
    return value == null || value.trim().length() == 0 ? null : value;
  }

  /**
   * 处理selecKey注解 生成相应的KeyGenerator对象
   * */
  private KeyGenerator handleSelectKeyAnnotation(SelectKey selectKeyAnnotation, String baseStatementId, Class<?> parameterTypeClass, LanguageDriver languageDriver) {
    // 获得各个属性
    String id = baseStatementId + SelectKeyGenerator.SELECT_KEY_SUFFIX;
    Class<?> resultTypeClass = selectKeyAnnotation.resultType();
    StatementType statementType = selectKeyAnnotation.statementType();
    String keyProperty = selectKeyAnnotation.keyProperty();
    String keyColumn = selectKeyAnnotation.keyColumn();
    boolean executeBefore = selectKeyAnnotation.before();

    // defaults
    boolean useCache = false;
    KeyGenerator keyGenerator = NoKeyGenerator.INSTANCE;
    Integer fetchSize = null;
    Integer timeout = null;
    boolean flushCache = false;
    String parameterMap = null;
    String resultMap = null;
    ResultSetType resultSetTypeEnum = null;

    // 创建 SqlSource对象
    SqlSource sqlSource = buildSqlSourceFromStrings(selectKeyAnnotation.statement(), parameterTypeClass, languageDriver);
    SqlCommandType sqlCommandType = SqlCommandType.SELECT;
    // 创建MapperStatement对象
    assistant.addMappedStatement(id, sqlSource, statementType, sqlCommandType, fetchSize, timeout, parameterMap, parameterTypeClass, resultMap, resultTypeClass, resultSetTypeEnum,
        flushCache, useCache, false,
        keyGenerator, keyProperty, keyColumn, null, languageDriver, null);

    // 获得 SelectKeyGenerator 的编号，格式为 `${namespace}.${id}`
    id = assistant.applyCurrentNamespace(id, false);

    MappedStatement keyStatement = configuration.getMappedStatement(id, false);
    // 创建SelectKeyGenerator对象 添加到keyGenerators集合中
    SelectKeyGenerator answer = new SelectKeyGenerator(keyStatement, executeBefore);
    configuration.addKeyGenerator(id, answer);
    return answer;
  }

}
