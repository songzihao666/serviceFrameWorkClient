package com.song.common.client;

import com.song.common.model.Result;

public interface ServiceClient {
	
	public Result doService(int type, String jdata, byte[] data);

}
