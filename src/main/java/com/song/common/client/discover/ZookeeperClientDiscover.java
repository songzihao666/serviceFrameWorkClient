package com.song.common.client.discover;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.song.common.client.imp.ServiceClientImp;

public class ZookeeperClientDiscover {
	
	private static Logger logger = LoggerFactory.getLogger(ZookeeperClientDiscover.class);
	
	private ZooKeeper zooKeeper;
	
	private String serviceName;
	
	private Map<String, String> services;
	
	private ServiceClientImp serviceClient;
	
	public ZookeeperClientDiscover(ZooKeeper zooKeeper, String serviceName, ServiceClientImp serviceClient) throws Exception {
		this.zooKeeper = zooKeeper;
		this.serviceName = serviceName;
		this.serviceClient = serviceClient;
		findService();
	}

	private Watcher watcher = new Watcher() {
		
		@Override
		public void process(WatchedEvent event) {
			// TODO Auto-generated method stub
			if (EventType.NodeChildrenChanged.equals(event.getType())) {
				try {
					findService();
				} catch (Exception e) {
					// TODO: handle exception
					logger.error(e.getMessage(),e);
				}
			}
		}
	};
	
	private void findService() throws Exception {
		List<String> servers = zooKeeper.getChildren("/song/services/" + serviceName, watcher);
		services = new HashMap<>();
		for (String key : servers) {
			services.put(key, key);
		}
		serviceClient.refreshServiceClients(services);
	}

}
