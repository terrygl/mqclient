package com.hualu.cloud.simulation;

import org.apache.log4j.Logger;

import com.ehl.itgs.interfaces.callback.ServerStatusCallBack;
import com.hualu.cloud.clientinterface.realtime.WatchDogClient;

public class ssc implements ServerStatusCallBack{
	public static Logger logger = Logger.getLogger(ssc.class);
	public void	heartbeatCheck(java.lang.String status) {
		System.out.println("收到跳变信息："+status);		
		logger.warn("收到跳变信息："+status);
	}
}
