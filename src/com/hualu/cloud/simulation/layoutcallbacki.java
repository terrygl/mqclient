package com.hualu.cloud.simulation;

import java.util.ArrayList;

import com.ehl.itgs.interfaces.bean.Layout;
import com.ehl.itgs.interfaces.bean.QueryInfo;
import com.ehl.itgs.interfaces.callback.LayoutCallback;

public class layoutcallbacki implements LayoutCallback{
	public java.util.ArrayList<Layout> getLayout(){
		//准备QueryInfo和layout列表
		QueryInfo queryInfo=new QueryInfo();
		ArrayList<Layout> bean=new ArrayList<Layout>();
		
		queryInfo.setSessionId(Long.valueOf("1234567890"));
		queryInfo.setUserIp("10.2.111.222");
		queryInfo.setUserName("yhy");
		
		Layout l1=new Layout();
		l1.setPlateType("02");
		l1.setPlateNumber("鲁A12345");
		l1.setBegTime("2013-09-18 13:00:00");
		l1.setEndTime("2013-09-18 14:50:00");
		l1.setLayoutId("123");
		String[] tsgid1={"1","2"};
		l1.setTgs(tsgid1);
		Layout l2=new Layout();
		l2.setPlateType("02");
		l2.setPlateNumber("鲁A12346");
		l2.setBegTime("2013-09-18 13:00:00");
		l2.setEndTime("2013-09-18 15:00:00");
		l2.setLayoutId("234");
		String tsgid2[]={"3","4"};
		l2.setTgs(tsgid2);
		bean.add(l1);
		bean.add(l2);
		return bean;
	}
}
