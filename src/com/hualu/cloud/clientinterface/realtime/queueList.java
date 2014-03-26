package com.hualu.cloud.clientinterface.realtime;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Timer;

import org.apache.log4j.Logger;

import com.hualu.cloud.basebase.staticparameter;
import com.hualu.cloud.clientinterface.CloudFactory;
import com.hualu.cloud.db.memoryDB;
import com.rabbitmq.client.Address;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class queueList {
	static String hostnamelist=staticparameter.getValue("MessageHostList","host51:5672");
	public static String Queuelist=staticparameter.getValue("Queuelist","RequestCommand4");//命令消息队列
	public static int initstate=0;
	private static Connection connection; 
	public static Channel channel; 
	public static Logger logger = Logger.getLogger(queueList.class);
	static int timerInterval=staticparameter.getIntValue("timerInterval", 600000);//默认间隔10分钟
	
	//队列初始化
	public static void listinit(boolean addAlarm) throws IOException{
		/*消息服务器异常中断重启后会清空所有自定义的queue、exchange、topic
		 * 因此需要在每个程序中增加自定义queue、exchange、topic的初始化*/
		//确定消息队列的名字
		if(initstate==1){
			logger.info("队列已经初始化，不用再次初始化！");
			return;
		}
		initstate=1;

		
	   if(addAlarm==true){
		    //准备告警消息线程。
		    //设定告警消息队列侦听的线程
		    String warnQueue=staticparameter.getValue("warnQueue","warnQueue1");//获得告警队列名字
		    if(warnQueue.indexOf("NULL")<0){//如果warnQueue=NULL，表示不没有该队列,不需要起该监听进程
			    warnlistener ex = new warnlistener(warnQueue,1);   
			    Thread pThread = new Thread(ex);   
			    pThread.start(); 
			    
			    //设定定时，方便进行l心跳和layout的数据同步
			    Timer timer=new Timer();		    
			    try {
					timer.schedule(new WatchDogClient("Alarm"),60000,timerInterval);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					logger.warn("启动WatchDogClient异常：",e);
				}
			    logger.info("设置心跳定时任务，每隔"+timerInterval+"毫秒执行一次心跳操作");
			}
	   }else{
			for(int i=0;i<60;i++){
				connection=getConnection();	
				if(connection!=null)
					break;
				else{
					logger.error("消息服务器连接异常,休眠一秒后会继续链接，请核查！");
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			if(connection==null){
				logger.error("消息服务器重连了60次依然异常,MQCLient启动失败！请核查！");
				return;
			}
			//准备命令消息队列		   
		    channel = connection.createChannel();		   
		   
		    //通过配置可设定client要启动命令消息队列的名称列表。
		    logger.info("启动命令消息队列"+Queuelist);
		    if(Queuelist==null){
		    	logger.warn("配置文件缺少Queuelist项");
		    }
	    	channel.queueDeclare(Queuelist,false,false,true,null);//通过声明保证该队列存在
		    
		    //设定预警消息队列侦听的线程
		    String yjQueue=staticparameter.getValue("yujingQueue","yujingQueue1");//获得告警队列名字
		    if(yjQueue.indexOf("NULL")<0){//如果yjQueue=NULL，表示不没有该队列,不需要起该监听进程
			    warnlistener yj = new warnlistener(yjQueue,2);   
			    Thread pThreadyj = new Thread(yj);   
			    pThreadyj.start(); 
		    }
		    
		    //设定RTA告警消息队列侦听的线程
		    String RTAQueue=staticparameter.getValue("RTAQueue","RTAQueue1");//获得告警队列名字
		    if(RTAQueue.indexOf("NULL")<0){//如果RTAQueue=NULL，表示不没有该队列,不需要起该监听进程
			    warnlistener rta = new warnlistener(RTAQueue,3);   
			    Thread pThreadrta = new Thread(rta);   
			    pThreadrta.start(); 
		    }
		    
		    //设定套牌告警消息队列侦听的线程
		    String tpQueue=staticparameter.getValue("taopaiQueue","taopaiQueue1");//获得告警队列名字
		    if(tpQueue.indexOf("NULL")<0){//如果tpQueue=NULL，表示不没有该队列,不需要起该监听进程
			    warnlistener tp = new warnlistener(tpQueue,4);   
			    Thread pThreadtp = new Thread(tp);   
			    pThreadtp.start(); 
		    }
		    
		    //设定旅行时间消息队列侦听的线程
		    String ttQueue=staticparameter.getValue("TravelTimeQueue","TravelTimeQueue1");//获得告警队列名字
		    if(ttQueue.indexOf("NULL")<0){//如果ttQueue=NULL，表示不没有该队列,不需要起该监听进程
			    warnlistener tt = new warnlistener(ttQueue,5);   
			    Thread pThreadtt = new Thread(tt);   
			    pThreadtt.start(); 
		    }
		    
		    //设定定时，方便进行l心跳和layout的数据同步
		    Timer timer=new Timer();	
		    try{
		    	timer.schedule(new WatchDogClient("Tomcat"),60000,timerInterval);
		    }catch(Exception e){
		    	logger.warn("启动WatchDogClient异常：",e);
		    }
		    logger.info("设置心跳定时任务，每隔"+timerInterval+"毫秒执行一次心跳操作");
	   }
	}
	public static Connection getConnection(){//可满足1-n个rabbitmq节点时的connect创建
		hostnamelist=staticparameter.getValue("MessageHostList","host51:5672");
	    String[] hostarray=hostnamelist.split(",");
	    int i=hostarray.length;
	    
	    Address[] addrArr =new Address[i] ;
	     
	    for (int j = 0; j < i; j++){
	    	 String[] host =hostarray[j].split(":");
	    	 addrArr[j] = new Address(host[0],Integer.parseInt(host[1]));
	    }
		try{
			ConnectionFactory factory = new ConnectionFactory();
			return factory.newConnection(addrArr);
		}catch(IOException e) {
			// TODO Auto-generated catch block
			logger.warn("创建消息链接时异常："+e.getMessage());
			return null;
		}	     
	}
	
	public static Connection getConnection1(){//可满足1-4个rabbitmq节点时的connect创建
		String hostnamelist=staticparameter.getValue("MessageHostList","host51:5672");
		String[] hostarray=hostnamelist.split(",");
		logger.debug(hostnamelist+"  "+hostarray.length);
		int i=hostarray.length;
		try{
			ConnectionFactory factory = new ConnectionFactory();
			switch (i){
			case 2:
				String[] host21=hostarray[0].split(":");
				String[] host22=hostarray[1].split(":");
				Address[] addrArr2 = new Address[]{ new Address(host21[0], Integer.parseInt(host21[1]))
		        , new Address(host22[0], Integer.parseInt(host22[1]))};
				logger.debug("两个消息节点"+host21[0]+":"+host21[1]+","+host22[0]+":"+host22[1]);
				return factory.newConnection(addrArr2);
			case 3:
				String[] host31=hostarray[0].split(":");
				String[] host32=hostarray[1].split(":");
				String[] host33=hostarray[2].split(":");
				Address[] addrArr3 = new Address[]{ new Address(host31[0], Integer.parseInt(host31[1]))
		        , new Address(host32[0], Integer.parseInt(host32[1]))
				, new Address(host33[0], Integer.parseInt(host33[1]))};
				logger.debug("三个消息节点"+host31[0]+":"+host31[1]+","+host32[0]+":"+host32[1]+","+host33[0]+":"+host33[1]);
				return  factory.newConnection(addrArr3);
			case 4:
				String[] host41=hostarray[0].split(":");
				String[] host42=hostarray[1].split(":");
				String[] host43=hostarray[2].split(":");
				String[] host44=hostarray[3].split(":");
				Address[] addrArr4 = new Address[]{ new Address(host41[0], Integer.parseInt(host41[1]))
		        , new Address(host42[0], Integer.parseInt(host42[1]))
				, new Address(host43[0], Integer.parseInt(host43[1]))
				, new Address(host44[0], Integer.parseInt(host44[1]))};
				logger.debug("四个消息节点"+host41[0]+":"+host41[1]+","+host42[0]+":"+host42[1]+","+host43[0]+":"+host43[1]+","+host44[0]+":"+host44[1]);
				return factory.newConnection(addrArr4);
			default:
				String[] host11=hostarray[0].split(":");
				factory.setHost(host11[0]);
				logger.debug("一个消息节点"+host11[0]+":"+host11[1]);
				return factory.newConnection();
			}
		}catch(IOException e) {
			// TODO Auto-generated catch block
			logger.warn("创建消息链接时异常："+e.getMessage());
			return null;
		}
	}
	
}
