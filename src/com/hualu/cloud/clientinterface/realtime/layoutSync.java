package com.hualu.cloud.clientinterface.realtime;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.TimerTask;

import org.apache.log4j.Logger;

import redis.clients.jedis.Jedis;

import com.ehl.itgs.interfaces.bean.Layout;
import com.ehl.itgs.interfaces.bean.QueryInfo;
import com.google.gson.reflect.TypeToken;
import com.hualu.cloud.basebase.staticparameter;
import com.hualu.cloud.clientinterface.CloudFactory;
import com.hualu.cloud.db.memoryDB;

public class layoutSync extends TimerTask{
	public static Logger logger = Logger.getLogger(layoutSync.class);
	public static String msgsplit=staticparameter.getValue("msgsplit","@@");
	public static String parametersplit=staticparameter.getValue("parametersplit","##");
	public static Long MQServerValidInterval=Long.parseLong(staticparameter.getValue("MQServerValidInterval","60000"));
	public static boolean MQServerStatus=true;
	memoryDB mdb=new memoryDB();
	public void run() {  
		boolean status=true;//根据MQServer自己在内存数据库中记录的数据分析得到的结果
		boolean statusM=true;//根据MQClient往MQServer发送消息的回复来判断
		//想MQServer发送消息，看是否收到回复，判断后端是否OK
		//组织向后端MQServer心跳消息格式:AreYouReady@@servicename##ip##timestamp
		//获取本机IP地址和时间标签
		long currenttime=System.currentTimeMillis();
		String timestamp=String.valueOf(currenttime);
		InetAddress addr;
		String ip="";
		try {
			addr = InetAddress.getLocalHost();
			ip=addr.getHostAddress();//获得本机IP
		} catch (UnknownHostException e2) {
			// TODO Auto-generated catch block
			logger.warn("获取本机IP地址时遇到异常："+e2.getMessage());
		}

		String MSG="";
		if(CloudFactory.addAlarm==false)
			MSG="AreYouReady"+msgsplit+"MQClientT"+parametersplit//tomcat中运行MQClient
			   +ip+parametersplit+timestamp;
		else
			MSG="AreYouReady"+msgsplit+"MQClientA"+parametersplit//告警服务器中运行MQClient
			  +ip+parametersplit+timestamp;
		
		//启动消息命令线程处理(用回调函数可实现有返回值的线程)
		commander  ch= new commander(MSG); 
		
		logger.info("启动线程发送Layout命令消息："+MSG);
		try {
			statusM=ch.call();			
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			logger.warn("检测MQServer服务状态时遇到异常："+e1.getMessage());
		}	
		
		memoryDB m=new memoryDB();	
		//从memorydb中获取MQServer的状态，判断MQServer的状态和rabbitmq的状态
		String key="MQServer";
		String value=m.get(key);
		if(value==null){
			logger.info("尚未有发现MQServer注册");
		}else{
			String[] host=value.split(msgsplit);//@@
			for(int i=0;i<host.length;i++){
				String[] ss=host[i].split(parametersplit);
				long interval=(int)(currenttime-Long.parseLong(ss[1]));
				if(interval>MQServerValidInterval){
					//当前时间和历史时间差距超过10分钟，则视为MQServer有问题
					logger.error(ss[0]+"上运行的MQServer上次有效验证聚现在已经"+interval+"毫秒，请核查问题");
					status=false||status;//只要有一个为true即为true，因此用"或"的操作来记录MQServer的状态
				}else{
					//当前时间和历史时间差距不超过10分钟，则视为MQServer没有问题
					status=true||status;
				}
			}
		}

		//将MQClient（区分是运行在Alarm还是tomcat）的时间标签记录在memorydb		
		key="";
		if(CloudFactory.addAlarm==false)
			//MQClient运行在tomcat中
			key="MQClientT";
		else
			//MQClient运行在Alarm服务中
			key="MQClientA";
		
		value=m.get(key);
		if(value==null)
			//空值时，直接添加该key
			m.add(key,ip+parametersplit+timestamp);
		else{
			if(value.indexOf(ip)>=0){
				//非空值，且该key的value中有该ip的信息，则需要替换
				String[] ss=value.split(msgsplit);//@@
				String value0="";
				for(int i=0;i<ss.length;i++){
					if(ss[i].indexOf(ip)>=0){
						String[] sss=ss[i].split(parametersplit);//##
						ss[i]=sss[0]+parametersplit+timestamp;
					}
					if(i==0)
						value0=ss[i];
					else
						value0=value0+msgsplit+ss[i];
				}
				m.add(key, value0);
			}else{
				//非空值，但该key的value中有该ip的信息，则在该值后面直接添加
				m.add(key, value+msgsplit+ip+parametersplit+timestamp);					
			}
		}
		m.finalize();
		
		//综合判断MQServer的状态
		if(status==true||statusM==true){
			logger.info("MQServer 正常");
			if(MQServerStatus==false){
				//上次记录为false，这次判断为正常，则需要告知前端
				MQServerStatus=true;
			}
		}else{
			logger.error("MQClient前端检测无法连接MQServer，MQServer状态也未及时更新，请核查");
			if(MQServerStatus==true){
				//上次记录为false，这次判断为正常，则需要告知前端
				MQServerStatus=false;
			}
		}
		
		
		//如果layoutcallback存在，则从前端获取layout并进行比对
		if(CloudFactory.loc==null)
			logger.info("初始化时没有LayoutCallback接口，无法进行布控数据同步操作");
		else{
			ArrayList<Layout> all=CloudFactory.loc.getLayout();
			int i0=all.size();
			int i1=mdbCount("BKBH");
			if(i0==i1)
				logger.info("前后台布控记录数一致");
			else{
				logger.error("前后台布控记录数不一致，清除重部");
				int i2=mdbClear("BKBH");
				int i3=mdbClear("GJ");
				logger.info("清除BKBH条目数："+i2+"  清除GJ条目数："+i3);
				for(int i=0;i<i0;i++){
					Layout layout=all.get(i);
					String layoutid=layout.getLayoutId();
					try {
						layout(layout,mdb);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}					
				logger.info("完成重新布控");
			}
		}
	}
	
	public int mdbClear(String prefix){
    	//根据该前缀获取数据，计算条目，并清除
		Set<String> str=mdb.memorydb.keys(prefix+"*");
		int i0=str.size();
		String key,value;
		Iterator<String> it=str.iterator();
		while(it.hasNext()){
			key=it.next();
			mdb.del(key);
		}	
		str.clear();
		return i0;
	}
	public int mdbCount(String prefix){
    	//根据该前缀获取数据，计算条目，并清除
		Set<String> str=mdb.memorydb.keys(prefix+"*");
		int i0=str.size();
		return i0;
	}
	
	/**
	 * 暂时没用到
	 *  布控：key分为HPW前缀表示按照号牌号码和号牌类型比对告警，TSGW前缀表示按照卡口
	 *  1个布控编号对应一个车牌号。但一个车牌号可能对应多个布控编号。
	 * @param bean 布控条件集合
	 * @return true：成功,false:失败
	 * 特别说明：
	 * 2014年元月7日跟小高确认过:车牌类型必须指定，不会指定会全部的。车牌号对多两位为通配符，英文下划线表示。
	 * 如果没有通配符：key1=BKBH:bkbhid value1=GJ:platetypeplatenumber
	 * 				  key2=GJ:platetypeplatenumber value2=BKBH:bkbhid1@tgs=id1,tgs=id2...;BKBH:bkbhid2@tgs=id1,tgs=id2...(保证一个车牌可对应多个布控)
	 * 如果有通配符：key1保持不变，另再增加两类值
	 * 如果有一个通配符：key3=GJMH1	value3=1;GJ:platetypeplatenumber;BKBH:bkbhid;tgs=...@1;GJ:platetypeplatenumber@@5;GJ:platetypeplatenumber;BKBH:bkbhid;tgs=...
	 * 上面表示，第一位为通配符的有两条记录，第五位为通配符的有一条记录
	 * key4=GJMH2	value3=1,3;GJ:platetypeplatenumber;BKBH:bkbhid;tgs=...@3,6;GJ:platetypeplatenumber;BKBH:bkbhid;
	 * 上面表示，1、3为通配符的记录有一条，3、6为通配符的记录有一条。
	 * 	取消时根据BKBH和value值进行取消			
	 */
	public boolean layout(Layout layout,memoryDB memorydb) throws Exception {
		logger.info("前后端不一致，需要补充布控");
		String key1,value1,key2,value2,bkendtime,tgslist;
		String[] value;
		
		key1="BKBH:"+layout.getLayoutId();
		value1="GJ:"+layout.getPlateType()+layout.getPlateNumber();
		key2=value1;

		//获取监控范围：卡口数组，并将之转化为字符串tgs1,tgs2,tgs3...;
		value=layout.getTgs();
		tgslist="";
		if(value==null)
			tgslist="tgs=*";
		else{
			int j=0;
			for(j=0;j<value.length;j++){
				tgslist=tgslist+"tgs="+value[j]+",";
			}
		}
			
		String value3=memorydb.get(key2);//该号码原有的监控
		if(value3==null)
			value2=key1+"@"+tgslist;//该号码之前没有被监控
		else
			value2=value3+";"+key1+"@"+tgslist;//该号码之前有部门在监控
		int pos1=value1.indexOf("_");
		if(pos1>=0){
			int pos2=value1.indexOf("_", pos1+1);
			if(pos1>=0 && pos2>0){//两个通配符
				key2="GJM2";
					value2=memorydb.get(key2);
					if(value2==null)
						value2=String.valueOf(pos1)+","+String.valueOf(pos2)+";"+value1+";"+key1+";"+tgslist;
					else
						value2=value2+"@"+String.valueOf(pos1)+","+String.valueOf(pos2)+";"+value1+";"+key1+";"+tgslist;
				}else{//一个通配符
					key2="GJM1";
					value2=memorydb.get(key2);
					if(value2==null)
						value2=String.valueOf(pos1)+";"+value1+";"+key1+";"+tgslist;
					else
						value2=value2+"@"+String.valueOf(pos1)+";"+value1+";"+key1+";"+tgslist;
				}	
		}
		memorydb.add(key1, value1);
		memorydb.add(key2, value2);		
		logger.debug("key1:"+key1+";"+memorydb.get(key1));
		logger.debug("key2:"+key2+";"+memorydb.get(key2));
		return true;
	}
	
	/**
	 *  暂时没用到
	 *  撤控
	 * @param bean 布控条件集合
	 * @return true：成功,false:失败
	 */
	//可处理有通配符的布控报警的取消
	public boolean cancelLayout(Layout layout,memoryDB memorydb) throws Exception{
		String key1,key2;		
		logger.info("前端无后端有，需要补充撤控");
			/*
			第一个key是BKBH:布控编号,value是车牌类型+车牌种类，主要是便于撤控操作。
			第二个key为 GJ:号牌种类+号牌号码，value是TGS1,tgs2...tgsn;车辆信息+案件信息+布控信息
			*/
			key1="BKBH:"+layout.getLayoutId();
			key2="GJ:"+layout.getPlateType()+layout.getPlateNumber();
			
			String value1=memorydb.get(key1);//其实是GJ：车牌类型车牌号码”
			if(value1==null){//根据布控编号查询为空，表示该布控不存在
				logger.info(key1+" 该布控编号不存在!");
			}
			else{//布控编号查询不为空，则将要对得到的结果进行剔除或者删除处理
				//判断value1中是否有通配符
				int pos1=value1.indexOf("_");
				if(pos1<0){//无通配符
					logger.info(key1+" 无通配符撤控! "+key2);
					String value2=memorydb.get(value1);
					String value3="";
					if(value2==null) ;//如果结果为空，表示GJ布控记录不存在
					else{//如果结果不为空，则看是满足剔除条件，还是删除条件
						String[] list1=value2.split(";");//普通布控，多个记录间是用分号分割的。
						logger.info("无通配符撤控记录个数："+list1.length);
						if(list1.length==1)//只有一个记录，则直接删除
							memorydb.del(value1);
						else{//有GJ布控的记录中有多条信息，则需要剔除
							for(int j=0;j<list1.length;j++){
								if(list1[j].indexOf(key1)<0){
									if(value3.length()<1)
										value3=list1[j];
									else
										value3=value3+";"+list1[j];
								}
							}
							memorydb.add(value1,value3);//相当于覆盖
						}
					}
				}else{//有通配符
					int pos2=value1.indexOf("_", pos1+1);
					if(pos2<0){//有一个通配符
						logger.info(key1+" 一个通配符撤控! "+key2);
						String value3=memorydb.get("GJM1");
						String valuelist="";
						if(value3!=null){
							String[] ss=value3.split("@");
							for(int ii=0;ii<ss.length;ii++){
								System.out.println(ss[ii]);
								System.out.println(value1+";"+key1);
								if(ss[ii].indexOf(value1+";"+key1)<0){
									if(valuelist.length()<1)
										valuelist=ss[ii];
									else
										valuelist=valuelist+"@"+ss[ii];
								}
							}
							if(valuelist.length()<1)
								memorydb.del("GJM1");
							else
								memorydb.add("GJM1", valuelist);
						}
					}else{//有两个通配符
						logger.info(key1+" 两个通配符撤控! "+key2);
						String value3=memorydb.get("GJM2");
						String valuelist="";
						if(value3!=null){
							String[] ss=value3.split("@");
							for(int ii=0;ii<ss.length;ii++){
								logger.info(ss[ii]);
								logger.info(value1+";"+key1);
								if(ss[ii].indexOf(value1+";"+key1)<0){
									if(valuelist.length()<1)
										valuelist=ss[ii];
									else
										valuelist=valuelist+"@"+ss[ii];
								}
							}
							if(valuelist.length()<1)
								memorydb.del("GJM2");
							else
								memorydb.add("GJM2", valuelist);
						}
					}
				}					
				memorydb.del(key1);//根据布控编号删除键值对
			}//else			
		return true;
	}
}
