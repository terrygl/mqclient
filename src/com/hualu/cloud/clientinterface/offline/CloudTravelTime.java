package com.hualu.cloud.clientinterface.offline;

import org.apache.log4j.Logger;

import com.ehl.itgs.interfaces.TravelTimeInterface;
import com.ehl.itgs.interfaces.bean.QueryInfo;
import com.google.gson.Gson;
import com.hualu.cloud.clientinterface.CloudFactory;
import com.hualu.cloud.clientinterface.realtime.InterfaceRTA;
import com.hualu.cloud.clientinterface.realtime.commander;

public class CloudTravelTime implements TravelTimeInterface{
	public static Logger logger = Logger.getLogger(InterfaceRTA.class);
	
	/**
	 * 
	 *  分析路线卡口列表
	 * @param tgs 路线[卡口]
	 * @param travelOrientation 方向[方向]
	 * @param endTime 有效时间[时间]
	 * @param travelNo 路线编号
	 * @param bztgsj  应该通过的时间（分）
	 * @return	true：成功,false:失败
	 */
	
	public boolean addTravel(String[] tgs,String[] travelOrientation,String endTime, String travelNo,String bztgsj,QueryInfo queryInfo) 
				throws Exception{
		return true;
	}
	/**
	 * 
	 *  分析路线卡口列表
	 * @param tgs 路线[卡口]
	 * @param travelOrientation 方向[方向]
	 * @param endTime 有效时间[时间]
	 * @param travelNo 路线编号
	 * @param bztgsj  应该通过的时间（分）
	 * @return	true：成功,false:失败
	 */
	
	public boolean addTravel1(String[] tgs,String[] travelOrientation,String endTime, String travelNo,String bztgsj,QueryInfo queryInfo) throws Exception{
		//定义协议名称
		String MSG0="CloudTravelTime.addTravel";	
		//准备参数的文本串
		Gson gson=new Gson();

		String MSG1=gson.toJson(tgs);
		String MSG2=gson.toJson(travelOrientation);
		String MSG3=gson.toJson(endTime);
		String MSG4=gson.toJson(travelNo);
		String MSG5=gson.toJson(bztgsj);
		String MSG6=gson.toJson(queryInfo);
		
		//组织消息格式
		String MSG=MSG0+CloudFactory.msgsplit+MSG1
				+CloudFactory.parametersplit+MSG2
				+CloudFactory.parametersplit+MSG3
				+CloudFactory.parametersplit+MSG4
				+CloudFactory.parametersplit+MSG5
				+CloudFactory.parametersplit+MSG6;
		
		//启动消息命令线程处理(用回调函数可实现有返回值的线程)
		System.out.println("CloudTravelTime.java query()发送命令消息："+MSG);
		commander ch= new commander(MSG); 

		logger.info("启动线程发送addTravel命令消息："+MSG);
		return ch.call();
	}

}
