package com.hualu.cloud.clientinterface.realtime;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.TimerTask;

import com.ehl.itgs.interfaces.bean.Layout;
import com.ehl.itgs.interfaces.bean.QueryInfo;
import com.ehl.itgs.interfaces.callback.ServerStatusCallBack;
import com.google.gson.reflect.TypeToken;
import com.hualu.cloud.basebase.staticparameter;
import com.hualu.cloud.clientinterface.CloudFactory;
import com.hualu.cloud.db.memoryDB;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.GetResponse;
import org.apache.log4j.Logger;
public class WatchDogClient extends TimerTask  {	
	private final static String QUEUE_NAME_REQUEST="WATCHDOGREQUEST2";
	public static Logger logger = Logger.getLogger(WatchDogClient.class);
	memoryDB mdb=new memoryDB();
	private String session =null;
	private String hostname =null; //get the host address
	public static int testInt = 0;
	public static boolean serverState = true;//default is normal，if it is fault,it will be notified.
	int pulseMultiple=staticparameter.getIntValue("pulseMultiple", 10);//默认布控信息更新频度是心跳频度的10倍
	int pulseMulttpleCount=0;
	int pulseTimeout=staticparameter.getIntValue("pulseTimeout", 660000);//默认两个时间点超过的11分钟就认为服务超时
	public  WatchDogClient(String str)throws Exception {
		session =String.valueOf(System.currentTimeMillis());
		InetAddress netAddress = InetAddress.getLocalHost();
		hostname = netAddress.getHostName();
		if(str=="Alarm"){
			hostname=hostname+":Alarm";
		}
		else if(str=="Tomcat"){
			hostname = hostname+":Tomcat";
		}		
		initializeRedis(str);
	}
	
	public void run(){
		logger.error("进入心跳检测定时程序！");
		//心跳
		try{
			watch();
		}
		catch(IOException e){
			logger.warn("检测心跳时出现异常："+e.getMessage());
		}
		//告警服务中只进行心跳检测，不进行同步操作
		if(CloudFactory.addAlarm==false){
			//布控信息同步，按心跳频率的倍数操作
			pulseMulttpleCount++;
			if(pulseMulttpleCount>=pulseMultiple){
				logger.debug("准备进行layout同步操作");
				layoutSyn();
				logger.debug("准备进行layout同步操作结束");
				pulseMulttpleCount=0;
			}
		}
		logger.debug("结束心跳检测定时程序！");
	}

	public void watch()throws IOException{
		//创建链接工厂
		Connection connection = queueList.getConnection();
		if(connection==null){
			logger.error("rabbitmq connection is fault");
			return;
		}
		//create channel
		try{
		Channel channel = connection.createChannel();
		if(channel==null){
			logger.error("rabbitmq channel is fault ");
			return ;
		}
		//declare a queue
		channel.queueDeclare(QUEUE_NAME_REQUEST,false,false,false,null);
		//decelare temp queue
		String queueTemp = channel.queueDeclare().getQueue();
		//declare message and set message format
		//生成的消息队列名字
		//完整的命令消息
		//"queue name"+"@@"+"session"+"@@"+'?' 
		//-------staticparameter.msgsplit
		String message=queueTemp+"@@"+session+"@@"+hostname+"@@"+'?';
		//send message
		try{
			channel.basicPublish("",QUEUE_NAME_REQUEST, null,message.getBytes() );
			logger.error("发送心跳消息正常："+message);
		}
		catch(IOException e){
			channel.close();
			connection.close();
			logger.error("心跳消息发送异常："+e.getMessage());
		}
		//receive message
		long starttime=System.currentTimeMillis();				
		while(true){//每十秒循环一次
			GetResponse response=channel.basicGet(queueTemp,true);//获取消息
			if(response==null){
				try{
					Thread.sleep(100);//如果没有消息，则休眠100毫秒后再获取消息
				}
				catch(Exception e){
					logger.warn("心跳程序休眠时出现异常："+e.getMessage());
				}
			}
			else{
				String responseMsg=new String(response.getBody());
				logger.info("收到服务器端回复："+responseMsg+"xxx");
				if(serverState==false){
					if(CloudFactory.ssc!=null)
						CloudFactory.ssc.heartbeatCheck("2");//服务器端从异常恢复为正常
					logger.info("服务器恢复运行recover");
				}
				serverState=true;//server running
				break;
			}
			
			long endtime=System.currentTimeMillis();
			//判断读取rabbitmq是否超时
			//rabbitmq maybe down and test redis
			if(endtime-starttime>CloudFactory.responseTimeout){//默认30秒无返回则表示超时
				logger.warn("超过"+CloudFactory.responseTimeout+"毫秒没有收到服务器端的回复");
				//队列超时，读取redis的MQSserver最新时间，更新MQSserver状态
				String mqsIpStr = mdb.get("MQServers");//get mqserver ip address list
				if(mqsIpStr==null){
					logger.warn("redis has no key = MQServers，请核查后台服务是否运行、内存数据库是否运行、消息服务器是否正常运行！");
					if(serverState==true){
						if(CloudFactory.ssc!=null)
							CloudFactory.ssc.heartbeatCheck("1");//服务器端从正常到异常
						logger.warn("服务器运行出现故障，请核查后台服务是否运行、内存数据库是否运行、消息服务器是否正常运行！");
					}
					serverState=false;
					return ;
				}
				else if(mqsIpStr=="ERRORERROR"){
					if(serverState==true){
						if(CloudFactory.ssc!=null)
							CloudFactory.ssc.heartbeatCheck("1");//服务器端从正常到异常
						logger.warn("服务器运行出现故障，请核查后台服务是否运行、内存数据库是否运行、消息服务器是否正常运行！");
					}
					serverState=false;
					logger.warn("redis get key = MQServers false，ERRORERROR。请核查后台服务是否运行、内存数据库是否运行、消息服务器是否正常运行！");
					return ;
				}
				String[]mqsIpList =   mqsIpStr.split(";");
				int mIpNum = mqsIpList.length;
				boolean mqsState  = false;
				String mqsRunStr = null;
				int i=0;
				while(i<mIpNum){
					String mqsTimeKey = "MQS:"+mqsIpList[i];
					String mqsTime = mdb.get(mqsTimeKey);
					long timeStamp = Long.parseLong(mqsTime);
					if(endtime-timeStamp<pulseTimeout){//MQServer running normal 
						mqsState = true;
						mqsRunStr=mqsRunStr+mqsIpList[i]+":";//set running mqservers ip
					}
					i++;
				}
				if(mqsRunStr!=null){
					//write mqsRunStr to readis
					mdb.add("MQSRunnig", mqsRunStr);
				}
				if(serverState!=mqsState){
					//use callback
					//------circulation continue------
					ServerStatusCallBack callback= CloudFactory.ssc;
					if(mqsState){
						if(CloudFactory.ssc!=null)
							callback.heartbeatCheck("2");//恢复正常
						logger.info("后台服务恢复正常运行，recover");
					}
					else{
						if(CloudFactory.ssc!=null)
							callback.heartbeatCheck("1");//出现异常
						logger.info("后台服务出现存查还能，请核查消息服务器和后台服务，exception");
					}
					serverState = mqsState;
				}				
				//队列超时后，检测一遍后退出
				break;//中断循环，退出接收
			}
		}
		channel.queueDelete(queueTemp);//删除消息队列
		channel.close();//关闭响应消息通道
		}
		catch(Exception e){
			logger.error("创建消息通道失败");
		}
		connection.close();//关闭通道连接
	}

	private void initializeRedis(String str){
		// exchange name
		String warnExchange = staticparameter.getValue("warnExchange",null);
		// queue name
		String WarnQueue = staticparameter.getValue("warnQueue",null);
		String YJQueue = staticparameter.getValue("yujingQueue",null);
		String RTAQueue =staticparameter.getValue("RTAQueue",null);
		String TTQueue = staticparameter.getValue("TravelTimeQueue",null);
		String TPQueue = staticparameter.getValue("taopaiQueue",null);
		// topic name
		String WarnQueueTopic = staticparameter.getValue(WarnQueue,null);
		String YJQueueTopic = staticparameter.getValue(YJQueue,null);
		String RTAQueueTopic = staticparameter.getValue(RTAQueue,null);
		String TTQueueTopic = staticparameter.getValue(TTQueue,null);
		String TPQueueTopic = staticparameter.getValue(TPQueue,null);
		//initialize key and value

		if(str=="Alarm"){
			String clientAlarmListKey = "clientAlarmList";
			String clientAlarmListName = hostname;
			String exchangeAlarmListKey = clientAlarmListName+":exchange";
			String exchangeAlarmListName = warnExchange;
			String queueAlarmKey = clientAlarmListName+":Queue";
			String queueAlarmValue = WarnQueue;
			String topicsAlarmKey = clientAlarmListName+":Topics";
			String topicsAlarmValue = WarnQueueTopic;
			String alarmValue = mdb.get(clientAlarmListKey);
			//check ,read and write redis 
			if(alarmValue!=null){
				String alarmArr[] = alarmValue.split(";");
				//check
				int i=0;
				for(;i<alarmArr.length;i++){
					if(alarmArr[i].compareTo(clientAlarmListName)==0){
						alarmValue=alarmValue;
						break;
					}
				}
				if(i>=alarmArr.length){
					alarmValue=alarmValue+";"+clientAlarmListName;				
				}
			}
			else{
				alarmValue=clientAlarmListName;
			}
			//add Alarm hostname
			mdb.add(clientAlarmListKey ,alarmValue);
			//add Alarm exchange
			mdb.add(exchangeAlarmListKey,exchangeAlarmListName);
			//add Alarm queue
			mdb.add(queueAlarmKey,queueAlarmValue);
			//add Alarm topics
			mdb.add(topicsAlarmKey,topicsAlarmValue);
		}
		else if(str=="Tomcat"){
		String clientTomcatListKey = "clientTomcatList";
		String clientTomcatListName = hostname;
		String exchangeTomcatListKey = clientTomcatListName+":exchange";
		String exchangeTomcatListName = warnExchange;
		String queueTomcatKey = clientTomcatListName+":Queue";
		String queueTomcatValue =YJQueue+";"+RTAQueue+";"+TTQueue+";"+TPQueue;
		String topicsTomcatKey = clientTomcatListName+":Topics";
		String topicsTomcatValue = YJQueueTopic+";"+RTAQueueTopic+";"+TTQueueTopic+";"+TPQueueTopic;
		String tomcatValue = mdb.get(clientTomcatListKey);
		if(tomcatValue!=null){
			String tomcatArr[] = tomcatValue.split(";");
			//check
			int i=0;
			for(;i<tomcatArr.length;i++){
				if(tomcatArr[i].compareTo(clientTomcatListName)==0){
					break;
				}
			}
			if(i>=tomcatArr.length){
				tomcatValue = tomcatValue +";"+clientTomcatListName;
			}
		}
		else{
			tomcatValue=clientTomcatListName;
		}
		logger.info(tomcatValue);
		//add tomcat
		mdb.add(clientTomcatListKey, tomcatValue);
		//add exchange
		mdb.add(exchangeTomcatListKey, exchangeTomcatListName);
		//add queue
		mdb.add(queueTomcatKey, queueTomcatValue);
		//add topics
		mdb.add(topicsTomcatKey, topicsTomcatValue);
		}
	}
	
	public void layoutSyn(){
		memoryDB mdb=new memoryDB();
		//如果layoutcallback存在，则从前端获取layout并进行比对
		if(CloudFactory.loc==null)
			logger.info("初始化时没有LayoutCallback接口，无法进行布控数据同步操作");
		else{
			ArrayList<Layout> all=CloudFactory.loc.getLayout();
			if(all==null){
				logger.error("通过布控回调接口获取布控信息为空！请核查是否合理！");
				return;
			}else{				
				int i0=all.size();
				int i1=mdbCount("BKBH");
				if(i1<0){
					mdb.finalize();
					return;
				}
				if(i0==i1)
					logger.info("前后台布控记录数一致");
				else{
					logger.error("前后台布控记录数不一致，清除重部");
					int i2=mdbClear("BKBH");
					if(i2<0){
						mdb.finalize();
						return;
					}
					int i3=mdbClear("GJ");
					if(i3<0){
						mdb.finalize();
						return;
					}
					logger.info("清除BKBH条目数："+i2+"  清除GJ条目数："+i3);
					for(int i=0;i<i0;i++){
						Layout layout=all.get(i);
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
		mdb.finalize();
	}
	
	public void layoutSyn1(){//检测出前端比后端多的，重新布控，前端比后端少的，删除之
		memoryDB mdb=new memoryDB();
		//如果layoutcallback存在，则从前端获取layout并进行比对
		if(CloudFactory.loc==null)
			logger.info("初始化时没有LayoutCallback接口，无法进行布控数据同步操作");
		else{
			ArrayList<Layout> all=CloudFactory.loc.getLayout();
			int i0=all.size();
			try{
				Set<String> bkbhs=mdb.memorydb.keys("BKBH*");
				int i1=bkbhs.size();
				if(i0==i1)
					logger.info("前后台布控记录数一致");
				else{
					if(mdb.isConnected()==true){
						logger.warn("前后台布控记录数不一致，先核查前端有后端无的");
						for(int i=0;i<i0;i++){
							Layout layout=all.get(i);
							String bkbh=layout.getLayoutId();
							if(mdb.get(bkbh)==null)
								try {
									layout(layout,mdb);
									logger.info("前端有后端无补充布控");
								} catch (Exception e) {
									logger.warn("前端有后端无，但补充布控异常："+e.getMessage());
								}
						}
						logger.error("前后台布控记录数不一致，开始核查后端有前端无的");
						Iterator<String> it=bkbhs.iterator();
						for(int i=0;i<i1;i++){
							String bkbh=it.next();
							int state=0;
							for(int j=0;j<i0;j++){
								Layout layout=all.get(i);
								if(bkbh.indexOf(layout.getLayoutId())>=0)
									state=1;
							}
							if(state==0){//该布控编号前端没有，需要进行取消操作
								
							}
						}
					}					
					logger.info("完成重新布控");
				}
			}catch(Exception e){
				logger.error("布控信息同步过程中mdbclear出现异常",e);
			}
		}
		mdb.finalize();
	}
	
	public int mdbClear(String prefix){
    	//根据该前缀获取数据，计算条目，并清除
		try{
			Set<String> str=mdb.memorydb.keys(prefix+"*");
			int i0=str.size();
			String key;
			Iterator<String> it=str.iterator();
			while(it.hasNext()){
				key=it.next();
				mdb.del(key);
			}	
			str.clear();
			return i0;
		}catch(Exception e){
			logger.error("布控信息同步过程中mdbclear出现异常",e);
			mdb.reconnectMDB();
			return -1;
		}
	}
	public int mdbCount(String prefix){
    	//根据该前缀获取数据，计算条目，并清除
		try{
			Set<String> str=mdb.memorydb.keys(prefix+"*");
			int i0=str.size();
			return i0;
		}catch(Exception e){
			logger.error("布控信息同步过程中mdbcount出现异常:",e);
			mdb.reconnectMDB();
			return -1;
		}
	}
	
	/**
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
		logger.info("前、后端不一致，需要补充布控");
		String key1,value1,key2,value2,tgslist;
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
	 * 
	 *  撤控
	 * @param bean 布控条件集合
	 * @return true：成功,false:失败
	 */
	//可处理有通配符的布控报警的取消
	public boolean cancelLayout(Layout layout,memoryDB memorydb) throws Exception{
		String key1,key2;
		

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