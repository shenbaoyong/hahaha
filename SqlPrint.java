package com.mybatis.demo.demo;

import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.util.List;
import java.util.Properties;

import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.ParameterMode;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandlerRegistry;

public class SqlPrint implements Interceptor{
	
	private static final Log log = LogFactory.getLog(SqlPrint.class);

	public Object intercept(Invocation invocation) throws Throwable {
		Object target = invocation.getTarget();
		Class<? extends Object> claszz = target.getClass();
		Field boundSqlField = claszz.getDeclaredField("boundSql");
		boundSqlField.setAccessible(true);
		BoundSql boundSql = (BoundSql)boundSqlField.get(target);
		Field parameterObjectField = claszz.getDeclaredField("parameterObject");
		parameterObjectField.setAccessible(true);
		Object parameterObject = parameterObjectField.get(target);
		
		Field typeHandlerRegistryField = claszz.getDeclaredField("typeHandlerRegistry");
		typeHandlerRegistryField.setAccessible(true);
		TypeHandlerRegistry typeHandlerRegistry = (TypeHandlerRegistry) typeHandlerRegistryField.get(target);
		
		Field configurationField = claszz.getDeclaredField("configuration");
		configurationField.setAccessible(true);
		Configuration configuration = (Configuration) configurationField.get(target);
		
		String sql = boundSql.getSql();
		sql = sql.replaceAll("&lt;", "<");
		sql = sql.replaceAll("&gt;", ">");
		sql = sql.replaceAll("&lt;&gt;", "!=");
		
//		ErrorContext.instance().activity("setting parameters").object(mappedStatement.getParameterMap().getId());
	    List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
	    if (parameterMappings != null) {
	      for (int i = 0; i < parameterMappings.size(); i++) {
	        ParameterMapping parameterMapping = parameterMappings.get(i);
	        if (parameterMapping.getMode() != ParameterMode.OUT) {
	          Object value;
	          String propertyName = parameterMapping.getProperty();
	          if (boundSql.hasAdditionalParameter(propertyName)) { // issue #448 ask first for additional params
	            value = boundSql.getAdditionalParameter(propertyName);
	          } else if (parameterObject == null) {
	            value = null;
	          } else if (typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())) {
	            value = parameterObject;
	          } else {
	            MetaObject metaObject = configuration.newMetaObject(parameterObject);
	            value = metaObject.getValue(propertyName);
	          }
//	          TypeHandler typeHandler = parameterMapping.getTypeHandler();
	          JdbcType jdbcType = parameterMapping.getJdbcType();
	          if (value == null && jdbcType == null) {
	            jdbcType = configuration.getJdbcTypeForNull();
	          }
	          
	          sql = sql.replace("?", convertValue(value));
	          log.debug("THE FULL SQL ---------> " + sql);
	        }
	      }
	    }
		return invocation.getMethod().invoke(invocation.getTarget(), invocation.getArgs());
	}
	
	private String convertValue(Object val){
		String result = "";
		Class<? extends Object> clazz = val.getClass();
		if(String.class.equals(clazz)){
			return "'" + val + "'";
		}else if(Timestamp.class.equals(clazz)){
			return "to_date('" + val.toString() + "','yyyy-mm-dd hh24:mi:ss')";
		}else {
			return val.toString();
		}
	}

	public Object plugin(Object target) {
		return Plugin.wrap(target, this);
	}

	public void setProperties(Properties properties) {
		
	}

}
