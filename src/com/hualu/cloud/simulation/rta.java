package com.hualu.cloud.simulation;

import com.ehl.itgs.interfaces.bean.QueryInfo;
import com.ehl.itgs.interfaces.bean.ResultCar;
import com.ehl.itgs.interfaces.callback.RTACallback;

public class rta implements RTACallback{
	 public boolean	setRTAInfo(ResultCar car, QueryInfo queryInfo){
		 return true;
	 }
	 public  boolean	setWarnInfo(ResultCar car, java.util.HashMap<java.lang.Long,java.util.HashSet<java.lang.String>> map){
		 return true;
	 }
}
