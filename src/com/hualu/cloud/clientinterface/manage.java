package com.hualu.cloud.clientinterface;

import java.io.IOException;
import java.lang.reflect.Field;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;

import com.ehl.itgs.interfaces.QueryInterface;
import com.ehl.itgs.interfaces.TravelTimeInterface;
import com.ehl.itgs.interfaces.bean.Alarm;
import com.ehl.itgs.interfaces.bean.BackResult;
import com.ehl.itgs.interfaces.bean.CarQueryBean;
import com.ehl.itgs.interfaces.bean.Layout;
import com.ehl.itgs.interfaces.bean.QueryCar;
import com.ehl.itgs.interfaces.bean.QueryInfo;
import com.ehl.itgs.interfaces.bean.RTABean;
import com.ehl.itgs.interfaces.bean.ResultCar;
import com.ehl.itgs.interfaces.bean.TgsQueryBean;
import com.ehl.itgs.interfaces.bean.WarnQueryBean;
import com.ehl.itgs.interfaces.callback.AlarmCallback;
import com.ehl.itgs.interfaces.callback.DuplicateCarCallback;
import com.ehl.itgs.interfaces.callback.RTACallback;
import com.ehl.itgs.interfaces.callback.TravelTimeCallback;
import com.ehl.itgs.interfaces.factory.FactoryInterface;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.hualu.cloud.basebase.loglog;
import com.hualu.cloud.basebase.staticparameter;
import com.hualu.cloud.clientinterface.offline.CloudQuery;
import com.hualu.cloud.clientinterface.realtime.InterfaceLayout;
import com.hualu.cloud.clientinterface.realtime.InterfaceRTA;
import com.hualu.cloud.clientinterface.realtime.queueList;
import com.hualu.cloud.db.memoryDB;
import com.hualu.cloud.simulation.alarmcallbacki;
import com.hualu.cloud.simulation.layoutcallbacki;
import com.hualu.cloud.simulation.rta;
import com.hualu.cloud.simulation.ssc;
import com.hualu.cloud.simulation.tp;
import com.hualu.cloud.simulation.tt;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class manage {
	public static void main(String args[]) throws IOException{

		System.out.println("MQClient manage.java  main:"+loglog.processname);
		//staticparameter.getValue("1", "1");
		System.out.println("1000:模拟前端tomcat应用调用");
		System.out.println("1001:模拟前端alarm应用调用");
		System.out.println("1002:单纯检测心跳同步");
		System.out.println("0:测试告警信息接收");
		System.out.println("100:测试预警、特殊车辆、旅行时间等信息接收");
		System.out.println("1:测试卡口通行车辆查询");
		System.out.println("2:测试旅行时间");
		System.out.println("3:测试布控");
		System.out.println("4:测试取消布控");
		System.out.println("5:测试预警布控");
		System.out.println("6:测试特殊车辆监控布控");
		System.out.println("7:测试卡口过车监控布控");
		System.out.println("8:测试预警、卡口过车监控、特殊车辆缉拿空布控的取消操作");
		System.out.println("9:测试MQServer状态跳变");
		System.out.println("10:准备预警、特殊车辆、卡口监控数据");
		System.out.println("11:从内存数据库获取现存的预警、特殊车辆、卡口监控数据");
		int i=0;
//		if(args.length==0)
//			return ;
//		else
//			i=Integer.parseInt(args[0]);
		i=1001;
		switch(i){
		case 1000:
			CloudFactory cf1=new CloudFactory();
			layoutcallbacki llc=new layoutcallbacki();
			cf1.addListener(llc);
			rta rta1=new rta();
			cf1.addListener(rta1);
			tp tp1=new tp();
			cf1.addListener(tp1);
			tt tt1=new tt();
			cf1.addListener(tt1);
			ssc ssc1=new ssc();
			cf1.addListener(ssc1);
			break;
		case 1001:
			CloudFactory cf2=new CloudFactory();
			alarmcallbacki ac=new alarmcallbacki();
			cf2.addListener(ac);
			ssc ssc2=new ssc();
			cf2.addListener(ssc2);
			break;
		case 1002:
			CloudFactory cf3=new CloudFactory();
			ssc ssc3=new ssc();
			cf3.addListener(ssc3);
			break;
		case 0:
			queueList.listinit(false);
			break;
		case 100:
			queueList.listinit(true);
			break;
		case 1:
			try {
				QueryTest();
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			break;
		case 2:
			try {
				TravelTimeTest();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			break;
		case 3:
			testlayout();
			break;
		case 4:
			testcancellayout("yhy");
			break;
		case 5://预警
			testRTA(3);
			break;
		case 6://特殊车辆
			testRTA(2);
			break;
		case 7://卡口
			testRTA(1);
			break;
		case 8://预警、特殊车辆、卡口取消监控
			testRTA(4);	
			break;
		case 9://MQServer状态跳变的监控
			//testRTA(1);
			break;
		case 10://准备预警、特殊车辆、卡口监控数据
			preparedata();	
			break;
		case 11://从获取内存数据库现存的预警、特殊车辆、卡口监控数据
			testsync();	
			break;
		default:
			System.out.println("请输入选项");
			break;
		}


					//卡口车辆查询
//					try {
//						QueryTest();
//					} catch (Exception e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
					
					//计算旅行时间
//					try {
//						TravelTimeTest();
//					} catch (Exception e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
	}
	
	 /********************************************************************/
    /*------------------------   计算旅行时间                 ---------------------*/
    /********************************************************************/
	public static void TravelTimeTest() throws Exception{
			System.out.println("测试旅行时间");
			FactoryInterface myinterface=new CloudFactory();
			TravelTimeInterface ttinterface=myinterface.createTravelTime();

			String[] tgs = {"10","18"};
			String[] travelOrientation = {"2","2"};
			String endTime = "20140108155900";
			String travelNo = "";
			String bztgsj = "";
			QueryInfo qi = new QueryInfo();
			
			try{
				if(ttinterface.addTravel(tgs, travelOrientation, endTime, travelNo, bztgsj, qi))
					System.out.println("sucess");
				else{
					System.out.println("fail");
				}
				}catch(Exception e){
					e.printStackTrace();
				}		
		}
	
	/********************************************************************/
	/*----------------------      卡口车辆查询                  --------------------*/
	/********************************************************************/
    public static void QueryTest() throws Exception {
		System.out.println("测试卡口通行车辆查询");
		
		FactoryInterface myinterface=new CloudFactory();
		QueryInterface queryinterface=myinterface.createQuery();
		
        QueryCar car = new QueryCar(); //查询条件，所有查询条件都是&&关系，即PASS-ALL
        QueryInfo qi = new QueryInfo();
        qi.setSessionId(1111116L);
        
    	/**
    	 * 车辆品牌。
    	 */
//        String[] carBrand = {"99"};
//        car.setCarBrand(carBrand);
    	
    	/**
    	 * 车辆颜色
    	 */
//        String[] carColor = {"1","5"};
//        car.setCarColor(carColor);

    	/**
    	 * 车牌颜色
    	 */
//        String[] plateColor = {"1","2"};
//        car.setPlateColor(plateColor);
        
        /**
    	 * 车牌号码
    	 */
        String plateNumber = "豫G[0-2]";   //正则表达式，支持模糊查询
        car.setPlateNumber(plateNumber);
        
        /**
    	 * 车牌种类
    	 */
//        String[] plateType = {"2"};
//        car.setPlateType(plateType);
       
        /**
    	 * 车道编号 
    	 */
//        String[] landIe = {"2","3","6","4"};
//        car.setLandIe(landIe);
        
        /**
         * 通行速度
         */
//       String[] speed = {"45","90"};            
//       car.setSpeed(speed);
       
        /**
         * 车行方向
         */
//        String[] travelOrientation = {"1","3","4"};
//        car.setTravelOrientation(travelOrientation);
        
        /**
         * 车辆状态
         */
//        String[] carState = {"1","2"};
//        car.setCarState(carState);
        
        /**
         * 地点编号
         */
//        String[] loncatonId = {"1","2"};
//        car.setLoncatonId(loncatonId);
        
        /**
         * 抓拍方向
         */
//        String[] captureDir = {"1","2"}; 
//        car.setCaptureDir(captureDir);
        
         
        //全部卡口
//        String[] tgs = {"1",155","156","157","158","159","160","161","182","183","184",
//        		"185","186","187","189","190","244","246","247","248","249","250","251",
//        		"252","253","254","255","256","257","258","259","260","261","262","263",
//        		"264","265","266","267","270","273","274","275","278","279","280","282",
//        		"284","285","286","287","288","289","290","293","453","465"};
//        String[] tgs = {"155","156","157","158","159","160","161","182","183","184"};
        String[] tgs = {"10","18","19","22"};
        String begTime = "2014-01-08 15:39:34"; 
        String endTime = "2014-01-08 16:39:34"; 
         
        try{
        	System.out.println("开始执行");
        	BackResult<ResultCar> br0 = queryinterface.query(car, tgs, begTime, endTime, 1, 80, qi);
        	if(br0 == null) {
        		System.out.println("br is null");
        		return;
        	}
            ResultCar rc = new ResultCar();
            ArrayList<ResultCar> al = new ArrayList<ResultCar>();
        	System.out.println("br0.totalNum="+br0.getTotalNum());
        	al=br0.getBeans();
        	if(al == null)
        	{
        		System.out.println("没有返回查询结果");
        		return;
        	}
        	int len=al.size();
        	for(int i=0;i<len;i++){
        		rc=al.get(i);
//        		System.out.println("PlateType="+rc.getPlateType()+" "+"LoncatonId="+rc.getLoncatonId()+" "+"CarColor="+rc.getCarColor());
//        		System.out.println("CarColor="+rc.getCarColor());
//        		System.out.println("PlateColor="+rc.getPlateColor());
//        		System.out.println("CarNumber="+rc.getPlateNumber()+"|PlateType="+rc.getPlateType()+"|travelOrientation="+rc.getTravelOrientation());
//        		System.out.println("PlateType="+rc.getPlateType());
//        		System.out.println("LandIe="+rc.getLandIe());
//        		System.out.println("Speed="+rc.getSpeed());
        		System.out.println("travelOrientation="+rc.getTravelOrientation()+" "+"carState="+rc.getCarState()+" "+"loncatonId="+rc.getLoncatonId()+" "+"captureDir="+rc.getCaptureDir());
        		
        	}
        	System.out.println("br0.totalNum="+br0.getTotalNum());       	
            System.out.println("执行完毕！");
        }catch(Exception e){
        	e.printStackTrace();
        }
    }
	
	public static void testRTA(int num){
		if(num==1){
			//卡口过车监控设置
			InterfaceRTA rta=new InterfaceRTA();
			RTABean bean=new RTABean("5",null,null);
			
			QueryInfo queryInfo=new QueryInfo();
					
			queryInfo.setSessionId(Long.valueOf("1234567890"));
			queryInfo.setUserIp("10.2.111.222");
			queryInfo.setUserName("yhy");
			
			try{
				if(rta.addTgs(bean, queryInfo))
					System.out.println("sucess");
				else{
					System.out.println("fail");
				}
			}catch(Exception e){
				e.printStackTrace();
			}
		}	
		if(num==2){
			//特殊车辆预警设置
		}
		if(num==3){
			//卡口预警设置
		}
	}
	
	public static void testcancellayout(String username){
		InterfaceLayout a=new InterfaceLayout();
		
		//准备QueryInfo和layout列表
		QueryInfo queryInfo=new QueryInfo();
		ArrayList<Layout> bean=new ArrayList<Layout>();
		
		queryInfo.setSessionId(Long.valueOf("1234567890"));
		queryInfo.setUserIp("10.2.111.222");
		queryInfo.setUserName(username);
		
		Layout l1=new Layout();
		l1.setLayoutId("1");
		l1.setPlateType("02");
		l1.setPlateNumber("京A12345");
		Layout l2=new Layout();
		l2.setPlateNumber("鲁A12345");
		l2.setPlateType("02");
		l2.setLayoutId("2");
		bean.add(l1);
		bean.add(l2);
		
		try {
				System.out.println(System.currentTimeMillis());
				a.layout(bean, queryInfo);//调用该方法就会发送相关命令消息
				System.out.println("调用执行完毕:"+System.currentTimeMillis());
				Thread.sleep(30000);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		

	}
	public static void query(String platetype){

		CloudQuery cq=new CloudQuery();
		
		//启动服务侧的命令消息侦听
		
		//准备输入参数
		QueryInfo queryInfo=new QueryInfo();
		ArrayList<Layout> layout=new ArrayList<Layout>();
		queryInfo.setSessionId(Long.valueOf("1234567890"));
		queryInfo.setUserIp("10.2.111.222");
		queryInfo.setUserName("yhy");
		
		QueryCar car=new QueryCar();
		String[] plateType={platetype};
		car.setPlateType(plateType);
		String[] plateColor={"0","1","2"};
		car.setPlateColor(plateColor);
		String[] carColor={"99","01"};
		car.setCarColor(carColor);
		String plateNumber="鲁A*79%";
		car.setPlateNumber(plateNumber);
		String[] bayonets={"3701979107","3701979102"};
		String begTime="2012-09-25 10:00:00";
		String endTime="2013-09-25 10:00:00";
		int begNum=1;
		int num=300;
		//发送查询命令
		try {
			cq.query(car, bayonets, begTime, endTime, begNum, num, queryInfo);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	public static void fromclass() throws ClassNotFoundException{
		String classPath="com.ehualu";
		classPath="java.lang.reflect.Field";
		Class cls = Class.forName(classPath); 
		Field[] fieldlist = cls.getDeclaredFields();
        for (int i = 0; i < fieldlist.length; i++) {
            Field fld = fieldlist[i];
            System.out.println("name = " + fld.getName());
            System.out.println("decl class = " + fld.getDeclaringClass());
            System.out.println("type = " + fld.getType());
            }

        
	}
	
	public static void fromto(){
		Gson gson=new Gson();
		QueryInfo queryInfo=new QueryInfo();
		ArrayList<Layout> layout=new ArrayList<Layout>();
		
		queryInfo.setSessionId(Long.valueOf("1234567890"));
		queryInfo.setUserIp("10.2.111.222");
		queryInfo.setUserName("yhy");
		
		String str=gson.toJson(queryInfo);
		QueryInfo qi=gson.fromJson(str, new TypeToken<QueryInfo>(){}.getType());
		System.out.println(str);
		System.out.println(qi.getSessionId());
		
		Layout l1=new Layout();
		l1.setLayoutId("123");
		String[] tsgid1={"1","2"};
		l1.setTgs(tsgid1);
		Layout l2=new Layout();
		l2.setLayoutId("234");
		String tsgid2[]={"3","4"};
		l2.setTgs(tsgid2);
		layout.add(l1);
		layout.add(l2);

		String str1=gson.toJson(layout);
		System.out.println("layout="+str1);
		ArrayList<Layout> ll=new ArrayList<Layout>();
		ll=gson.fromJson(str1, new TypeToken<ArrayList<Layout>>(){}.getType());

		System.out.println(str1);
		System.out.println("ArrayList<Layout> tgsid[0]"+ll.get(0).getTgs()[0]);
		System.out.println("ArrayList<Layout>"+ll.get(0).getTgs()[1]);
		
		//BackResult<Layout>
		 BackResult<Layout> bl=new BackResult<Layout>();
		 bl.setBeans(layout);
		 bl.setDsecription("yhytest");
		 bl.setStatus(1);
		 bl.setTotalNum(12345);
		 
			String msg=bl.getDsecription()+"@@";
			msg=msg+bl.getStatus()+"@@";
			msg=msg+bl.getTotalNum()+"@@";
			msg=msg+gson.toJson(bl.getBeans());
			System.out.println("msg = "+msg);
			
			BackResult<Layout> br=new BackResult<Layout>();
			String[] stra=msg.split("@@");
			System.out.println("stra[0]="+stra[0]);
			System.out.println("stra[0]="+stra[1]);
			System.out.println("stra[0]="+stra[2]);
			System.out.println("stra[0]="+stra[3]);
			br.setDsecription("stra[0]="+stra[0]);
			br.setStatus(Integer.valueOf(stra[1]));
			br.setTotalNum(Integer.valueOf(stra[2]));
			ArrayList<Layout> al=new ArrayList<Layout>();
			al=gson.fromJson(stra[3], new TypeToken<ArrayList<Layout>>(){}.getType());
			Layout la=al.get(0);
			la=al.get(1);
			System.out.println(la.getLayoutId());
			
			
		//BackResult<Layout> ll1=gson.fromJson(str, new TypeToken<BackResult<Layout>>(){}.getType());
		//System.out.println(ll1.getTotalNum());
		//ArrayList<Layout> la=ll1.getBeans();
		//if(la==null) System.out.println("empty");
		//System.out.println(ll1.getBeans().get(1).getBkbh());
		//System.out.println(ll1.getBeans().get(0).getTgsid()[1]);
		//System.out.println(ll1.getBeans().get(1).getTgsid()[0]);
		//System.out.println(ll1.getBeans().get(1).getTgsid()[1]);
	}
	
	public static void testmsg(){
		InterfaceLayout a=new InterfaceLayout();
		
		//准备QueryInfo和layout列表
		QueryInfo queryInfo=new QueryInfo();
		ArrayList<Layout> bean=new ArrayList<Layout>();
		
		queryInfo.setSessionId(Long.valueOf("1234567890"));
		queryInfo.setUserIp("10.2.111.222");
		queryInfo.setUserName("yhy");
		
		Layout l1=new Layout();
		l1.setLayoutId("123");
		String[] tsgid1={"1","2","3"};
		l1.setTgs(tsgid1);
		Layout l2=new Layout();
		l2.setLayoutId("234");
		String tsgid2[]={"3","4"};
		l2.setTgs(tsgid2);
		bean.add(l1);
		bean.add(l2);
		
		try {
			System.out.println(System.currentTimeMillis());
			a.layout(bean, queryInfo);//调用该方法就会发送相关命令消息
			System.out.println("第一次调用执行完毕");
			System.out.println(System.currentTimeMillis());
			a.layout(bean, queryInfo);//调用该方法就会发送相关命令消息
			System.out.println("第er次调用执行完毕");
			System.out.println(System.currentTimeMillis());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}  
	
	public static void testlayout(){
		InterfaceLayout a=new InterfaceLayout();
		
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
		
		try {
				System.out.println(System.currentTimeMillis());
				a.layout(bean, queryInfo);//调用该方法就会发送相关命令消息
				System.out.println("调用执行完毕:"+System.currentTimeMillis());
				//Thread.sleep(30000);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void preparedata(){

		memoryDB m=new memoryDB();
		
		//准备特殊车辆数据
		 //key1=ts:plateTypeplayteNumber
		 //value1=sessionid:SessionId1,SessionId2...
		 //key2=ts:sessiondid
		 //value2=ts:plateTypeplayteNumber
		m.add("ts:1京A12345", "sessionid:1,2,3");
		m.add("ts:12京A11111", "sessionid:3");
		m.add("ts:99京A22222", "sessionid:13,25,11111111");
		
		//准备卡口过程监控数据
		 //key1=kk:tgsid
		 //value1=cd:lanelist;fx:orientationlist;sessionid:SessionId1@@cd:lanelist;fx:orientationlist;sessionid:SessionId1...
		 //key2=kk:sessiondid
		 //value2=kk:tgsid
		m.add("kk:1", "cd:1,2,3;fx:1,2;sessionid:1@@cd:1,4;fx:1;sessionid:2@@cd:*;fx:*;sessionid:111");
		m.add("kk:567", "cd:*;fx:1,2;sessionid:21@@cd:4;fx:1;sessionid:22@@cd:*;fx:*;sessionid:31");
		m.add("kk:56", "cd:*;fx:1,2;sessionid:211");
		
		
		 //准备预警监控数据
		 //key1=yj:tgsid
		 //value1=yjtype:warnlist;cd:lanelist;fx:orientationlist;sessionid:SessionId1@@yjtype:warnlist;cd:lanelist;fx:orientationlist;sessionid:SessionId2...
		 //key2=yj:sessionid
		 //value2=tgsid1,tgsid2,.....tgsidn
		m.add("yj:12345", "1,2,3,4,5,6,7,8,9,10,11,12,13,14,15");
		m.add("yj:23456", "1,2,3");
		m.add("yj:22", "1,2,3");
		m.add("yj:1", "yjtype:1,2,3;cd:*;fx:1,2;sessionid:12345@@yjtype:2;cd:*;fx:*;sessionid:23456");
		m.add("yj:2", "yjtype:1,2,3;cd:*;fx:1,2;sessionid:12345@@yjtype:2;cd:*;fx:*;sessionid:23456");
		m.add("yj:3", "yjtype:1,2,3;cd:*;fx:1,2;sessionid:12345@@yjtype:2;cd:*;fx:*;sessionid:23456");
		m.add("yj:4", "yjtype:1,2,3;cd:*;fx:1,2;sessionid:12345");
		m.add("yj:5", "yjtype:1,2,3;cd:*;fx:1,2;sessionid:12345");
		m.add("yj:6", "yjtype:1,2,3;cd:*;fx:1,2;sessionid:12345");
		m.add("yj:7", "yjtype:1,2,3;cd:*;fx:1,2;sessionid:12345");
		m.add("yj:8", "yjtype:1,2,3;cd:*;fx:1,2;sessionid:12345");
		m.add("yj:9", "yjtype:1,2,3;cd:*;fx:1,2;sessionid:12345");
		m.add("yj:10", "yjtype:1,2,3;cd:*;fx:1,2;sessionid:12345");
		m.add("yj:11", "yjtype:1,2,3;cd:*;fx:1,2;sessionid:12345");
		m.add("yj:12", "yjtype:1,2,3;cd:*;fx:1,2;sessionid:12345");
		m.add("yj:13", "yjtype:1,2,3;cd:*;fx:1,2;sessionid:12345");
		m.add("yj:14", "yjtype:1,2,3;cd:*;fx:1,2;sessionid:12345");
		m.add("yj:15", "yjtype:1,2,3;cd:*;fx:1,2;sessionid:1");		
		
		m.finalize();
	}
	
	public static void testsync(){
		InterfaceRTA a=new InterfaceRTA();

		List<TgsQueryBean> c=a.queryTgs();
		System.out.println(c.size());
		for(int i=0;i<c.size();i++){
			System.out.println(c.get(i).getBean().getTgs()+";"+c.get(i).getBean().getLandIe()[0]+";"+c.get(i).getBean().getTravelOrientation()[0]+";"+c.get(i).getQueryInfo().getSessionId());
		}
		

		List<CarQueryBean> b=a.queryCar();
		for(int i=0;i<b.size();i++){
			System.out.println(b.get(i).getPlateNumber()+";"+b.get(i).getPlateType()+";"+b.get(i).getQueryInfo().getSessionId());
		}

		List<WarnQueryBean> d=a.queryWarn();
		for(int i=0;i<d.size();i++){
			RTABean[] aa=d.get(i).getBean();
			for(int j=0;j<aa.length;j++)				
				System.out.println(d.get(i).getQueryInfo().getSessionId()+";"+d.get(i).getWarnType()[0]+";"+aa[j].getTgs()+";"+aa[j].getLandIe()[0]+";"+aa[j].getTravelOrientation()[0]+";"+d.get(i).getQueryInfo().getSessionId());
		}
	}
	static void compute(double money,int year,double f){
		//compute(300000,60,0.02);
		double money0=money;
		double money1,zx=0;
		for(int i=1;i<year;i++){
			money1=money;
			money=money*(1+f);
			zx=money-money1;
			if(i>3)
				money=money-6500;
			System.out.println("第"+i+"年  总额："+money+" 息"+zx);
		}
	}	
}