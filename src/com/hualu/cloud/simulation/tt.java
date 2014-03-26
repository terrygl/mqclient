package com.hualu.cloud.simulation;

import com.ehl.itgs.interfaces.bean.QueryInfo;
import com.ehl.itgs.interfaces.bean.traveltime.LinkConfig;
import com.ehl.itgs.interfaces.bean.traveltime.TravelTimeRecord;
import com.ehl.itgs.interfaces.callback.TravelTimeCallback;

public class tt implements TravelTimeCallback{
	
	static int a = 0;
	static LinkConfig[] linkconfig = new LinkConfig[6];
	
	public void	addTravelTimeEstimationRecords(TravelTimeRecord[] travelTimeEstimationRecords) {
		
		for(TravelTimeRecord tter:travelTimeEstimationRecords) {
			String linkId = tter.getLinkId();
			String linkName = tter.getLinkName();
			int linkLength = tter.getLinkLenght();
			int actualTravelTime = tter.getActualTravelTime();
			int normalTravelTime = tter.getNormalTravelTime();
			int delayedTime = tter.getDelayedTime();
			double avgTravelSpeed = tter.getAvgTravelSpeed();
			String updateTime = tter.getUpdateTime();
			System.out.println("One record:"+linkId+"|"+linkName+"|"+linkLength+"|"+actualTravelTime+"|"+
								normalTravelTime+"|"+delayedTime+"|"+avgTravelSpeed+"|"+updateTime);
		}
		System.out.println("**************************************************");
		
	}
	
	public LinkConfig[] getAllLinkConfigs() {
		String[] linkId = {"5","2","3","1","6","4"}; //连线编号
		String[] linkName = {"A","B","C","D","E","F"};//连线名称
		for(int i=0; i<6; i++) {
			int linkLength = 26880; //连线长度，单位：米
			if(i == 0) {
				linkLength = 0;
			}
			 int normalTravelTime = 15;//标准通过时间, 单位：秒
			 double maxSpeed = 120.0;//限速，单位km/h
			 String startTgsId = "22";//起始卡口编号
			 switch(a){
			 	case 0:
			 		startTgsId = "22";
			 		a++;
			 		break;
			 	case 1:
			 		startTgsId = "23";
			 		a++;
			 		break;
			 	case 2:
			 		startTgsId = "25";
			 		a++;
			 		break;
			 	default:
			 		startTgsId = "26";
			 		a=0;
					break;
			}
			String startTgsDirectionId = "2";//起始卡口方向
			String endTgsId = "24";//结束卡口编号
			String endTgsDirectionId = "2";//结束卡口方向
			
			linkconfig[i] = new LinkConfig(null, null, 0, 0, 0.0, null, null, null, null);	
			linkconfig[i].setLinkId(linkId[i]);
			linkconfig[i].setLinkName(linkName[i]);
			linkconfig[i].setLinkLength(linkLength);
			linkconfig[i].setNormalTravelTime(normalTravelTime);
			linkconfig[i].setMaxSpeed(maxSpeed);
			linkconfig[i].setStartTgsId(startTgsId);
			linkconfig[i].setStartTgsDirectionId(startTgsDirectionId);
			linkconfig[i].setEndTgsId(endTgsId);
			linkconfig[i].setEndTgsDirectionId(endTgsDirectionId);				
		 }
		 return linkconfig;
	}

}
