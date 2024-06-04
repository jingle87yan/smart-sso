package com.smart.sso.client;

import com.smart.sso.client.filter.LoginFilter;
import com.smart.sso.client.filter.LogoutFilter;
import com.smart.sso.client.listener.LogoutListener;
import com.smart.sso.client.session.SessionMappingStorage;
import com.smart.sso.client.session.local.LocalSessionMappingStorage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.servlet.http.HttpSessionListener;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({ClientProperties.class})
public class ClientAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(SessionMappingStorage.class)
	public SessionMappingStorage sessionMappingStorage() {
		return new LocalSessionMappingStorage();
	}

	/**
	 * 单实例方式单点登出Listener
	 *
	 * @return
	 */
	@Bean
	@ConditionalOnMissingBean(name = "logoutListener")
	public ServletListenerRegistrationBean<HttpSessionListener> logoutListener(SessionMappingStorage sessionMappingStorage) {
		ServletListenerRegistrationBean<HttpSessionListener> listenerRegBean = new ServletListenerRegistrationBean<>();
		LogoutListener logoutListener = new LogoutListener(sessionMappingStorage);
		listenerRegBean.setListener(logoutListener);
		return listenerRegBean;
	}

	@Bean
	public FilterRegistrationBean<SmartContainer> smartContainer(ClientProperties properties) {
		SmartContainer smartContainer = new SmartContainer();
		smartContainer.setServerUrl(properties.getServerUrl());
		smartContainer.setAppId(properties.getAppId());
		smartContainer.setAppSecret(properties.getAppSecret());

		// 忽略拦截URL,多个逗号分隔
		smartContainer.setExcludeUrls(properties.getExcludeUrls());

		smartContainer.setFilters(new LogoutFilter(), new LoginFilter());

		FilterRegistrationBean<SmartContainer> registration = new FilterRegistrationBean<>();
		registration.setFilter(smartContainer);
		registration.addUrlPatterns("/*");
		registration.setOrder(1);
		registration.setName("smartContainer");
		return registration;
	}
}