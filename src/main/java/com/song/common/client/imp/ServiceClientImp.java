package com.song.common.client.imp;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.annotation.PostConstruct;

import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.curator.CuratorZookeeperClient;
import org.apache.curator.RetryPolicy;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.song.common.client.ServiceClient;
import com.song.common.client.discover.ZookeeperClientDiscover;
import com.song.common.client.hystrix.HystrixInvoker;
import com.song.common.client.pool.ServiceClientPool;
import com.song.common.model.Args;
import com.song.common.model.Result;
import com.song.common.model.ZipkinTraceContext;
import com.song.common.protocol.ServerService.Client;
import com.song.common.zipkin.TraceHelper;

import brave.Span;
import brave.propagation.TraceContext;

public abstract class ServiceClientImp implements ServiceClient {
	
	private static Logger logger = LoggerFactory.getLogger(ServiceClientImp.class);
	
	int roubin;
	
	@Value("${serviceName}")
	private String localServiceName;
	@Autowired
	private HystrixInvoker invoker;
		
	private static CuratorZookeeperClient zkClient;
	
	@Value("${zkServer}")
	private String zkServer;
	
	@Value("${kafkaServer}")
	private String kafkaServer;
	
	@Value("${workerThreads}")
	private int workerThreads;
	
	private Map<String,ObjectPool<Client>> serviceClients;
	
	private GenericObjectPoolConfig config = new GenericObjectPoolConfig();
	
	private ObjectPool<Client>[] realClients;
	
	private ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
	
	@PostConstruct
	private void init() throws Exception {
		config.setMaxTotal(workerThreads);
		config.setMinIdle(workerThreads);
		config.setMaxIdle(workerThreads);
		initZookeeper();
		new ZookeeperClientDiscover(ServiceClientImp.zkClient.getZooKeeper(), getServiceName(), this);
	}
	
	public abstract String getServiceName();
	
	private void initZookeeper() throws Exception {
		if (ServiceClientImp.zkClient == null) {
			RetryPolicy retryPolicy = new ExponentialBackoffRetry(3000, 3);
			ServiceClientImp.zkClient = new CuratorZookeeperClient(zkServer, 30000, 10000, null, retryPolicy);
			ServiceClientImp.zkClient.start();
		}
	}
	
	public void refreshServiceClients(Map<String, String> map) {
		
		if (serviceClients == null) {
			serviceClients = new HashMap<>();
			for (String host : map.values()) {
				String[] hosts = host.split(":");
				String ip = hosts[0];
				int port = Integer.valueOf(hosts[1]);
				ObjectPool<Client> pool = new GenericObjectPool<>(new ServiceClientPool(ip, port), config);
				serviceClients.put(host, pool);
			}
			realClients = serviceClients.values().toArray(new ObjectPool[0]);
		}else {
			for (String host : map.values()) {
				if (serviceClients.get(host) == null) {
					String[] hosts = host.split(":");
					String ip = hosts[0];
					int port = Integer.valueOf(hosts[1]);
					ObjectPool<Client> pool = new GenericObjectPool<>(new ServiceClientPool(ip, port), config);
					serviceClients.put(host, pool);
				}
			}
			for (String host : serviceClients.keySet()) {
				if (map.get(host) == null) {
					serviceClients.get(host).close();
					serviceClients.remove(host);
				}
			}
			
			try {
				readWriteLock.writeLock().lock();
				realClients = serviceClients.values().toArray(new ObjectPool[0]);
			}catch (Exception e) {
				// TODO: handle exception
				logger.error(e.getMessage(),e);
			} finally {
				// TODO: handle finally clause
				readWriteLock.writeLock().unlock();
			}	
		}
	}



	@Override
	public Result doService(int type, byte[] data) {
		// TODO Auto-generated method stub
		ObjectPool<Client> pool = null;
		Client client = null;
		try {
			pool = getPool();
			if (pool == null) {
				return new Result(505, "no service instance", null);
			}
			client = pool.borrowObject();			
			Span s1 = TraceHelper.getSpan();
			TraceContext context = s1.context();
			Long parentId = context.parentId();
			if (parentId == null) {
				parentId = 0L;
			}
			ZipkinTraceContext ctx = new ZipkinTraceContext(context.traceId(), context.spanId(), parentId,
					context.sampled(), context.debug());
			Args param = new Args(type, ByteBuffer.wrap(data), ctx);
			TraceHelper.csStart(s1);
			Result result = invoker.invoke(client, param);
			TraceHelper.crFinish(s1);
			return result;
		} catch (Exception e) {
			// TODO: handle exception
			logger.error(e.getMessage(),e);
			return new Result(500, e.getMessage(), null);
		}finally {
			try {
				if (client != null) {
					pool.returnObject(client);
				}
			} catch (Exception e2) {
				// TODO: handle exception
				logger.error(e2.getMessage(),e2);
			}
		}		
	}
	
	private ObjectPool<Client> getPool() {
		try {
			readWriteLock.readLock().lock();
			if (realClients.length == 0) {
				return null;
			}
			int index = roubin % realClients.length;
			roubin++;
			if (roubin > realClients.length) {
				roubin = 0;
			}
			return realClients[index];
		} finally {
			// TODO: handle finally clause
			readWriteLock.readLock().unlock();
		}
	}

}
