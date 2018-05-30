package com.song.common.client.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import com.song.common.zipkin.TraceContextHelper;
import com.song.common.zipkin.TraceHelper;

import brave.Span;

public class RequestInterceptor extends HandlerInterceptorAdapter {
	
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		// TODO Auto-generated method stub
		Span span = TraceHelper.getSpan();	
		span.name(request.getRequestURI());
		TraceContextHelper.setSpan(span);
		TraceHelper.csStart(span);
		TraceHelper.addInfo("url", request.getRequestURI());
		TraceHelper.addInfo("method", request.getMethod());
		TraceHelper.crFinish(span);
		return true;
	}
	
	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
			throws Exception {
		// TODO Auto-generated method stub
		TraceContextHelper.remove();
	}

}
