package com.song.common.client.hystrix;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;
import com.song.common.model.Args;
import com.song.common.model.Result;
import com.song.common.protocol.ServerService.Client;
@Component
public class HystrixInvoker {
	
	private static Logger logger = LoggerFactory.getLogger(HystrixInvoker.class);
	
	@HystrixCommand(fallbackMethod="getFallBack", threadPoolProperties={
			@HystrixProperty(name="coreSize", value="100")
		}, commandProperties= {@HystrixProperty(name="fallback.isolation.semaphore.maxConcurrentRequests", value="100")})
	public Result invoke(Client client, Args param) throws Exception {
		return client.doService(param);
	}
	
	public Result getFallBack(Client client, Args param, Throwable e) throws Exception {
		logger.error(e.getMessage(),e);
		return new Result(503, "server too buzy", null);
	}

}
