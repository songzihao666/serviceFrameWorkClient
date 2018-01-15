package com.song.common.client.pool;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFastFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;

import com.song.common.protocol.ServerService;
import com.song.common.protocol.ServerService.Client;

public class ServiceClientPool extends BasePooledObjectFactory<Client> {
	
	private String host;
	private int port;

	public ServiceClientPool(String host, int port) {
		this.host = host;
		this.port = port;
	}

	@Override
	public Client create() throws Exception {
		// TODO Auto-generated method stub
		TTransport transport = new TFastFramedTransport(new TSocket(host, port));
		TProtocol protocol = new TCompactProtocol(transport);
		final ServerService.Client client = new ServerService.Client(protocol);
		transport.open();
		return client;
	}

	@Override
	public PooledObject<Client> wrap(Client client) {
		// TODO Auto-generated method stub
		return new DefaultPooledObject<Client>(client);
	}
	
	@Override
	public void destroyObject(PooledObject<Client> p) throws Exception {
		// TODO Auto-generated method stub
		p.getObject().getOutputProtocol().getTransport().close();
	}

}
