package com.hualu.cloud.simulation;

import java.util.ArrayList;

import com.ehl.itgs.interfaces.bean.Alarm;
import com.ehl.itgs.interfaces.callback.AlarmCallback;

public class alarmcallbacki implements AlarmCallback{
	public void setAlarmInfo(ArrayList<Alarm> alarm){
		System.out.println("AlarmCallbak OK");
		return;
	}

}
