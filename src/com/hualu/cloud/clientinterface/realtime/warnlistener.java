package com.hualu.cloud.clientinterface.realtime;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.log4j.Logger;

import com.ehl.itgs.interfaces.bean.Alarm;
import com.ehl.itgs.interfaces.bean.DuplicateCar;
import com.ehl.itgs.interfaces.bean.QueryInfo;
import com.ehl.itgs.interfaces.bean.ResultCar;
import com.ehl.itgs.interfaces.bean.traveltime.TravelTimeRecord;
import com.hualu.cloud.basebase.staticparameter;
import com.hualu.cloud.clientinterface.CloudFactory;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.QueueingConsumer;

public class warnlistener implements Runnable{
	public String warnQueueName;
	int warntype;
	public static Logger logger = Logger.getLogger(warnlistener.class);
    public warnlistener(String queuename,int type) {   
    	super();
        this.warnQueueName=queuename;
        this.warntype=type;
        if(CloudFactory.addAlarm==true)
        	logger.info("告警服务加载的MQClient告警消息侦听线程启动");
        else
        	logger.info("前端应用加载的MQClient相关预警、特殊车辆监控、卡口过车监控、旅行时间等消息侦听线程启动");
    }   
    
    @Override  
    public void run() {   
        // TODO Auto-generated method stub 
		Connection connection=null;
		Channel channel=null;
		while(true){
			logger.info("warnlistener开始连接消息服务器，声明队列"+warnQueueName+"，绑定topic");
     		//由于每次消息服务器中断重启后会将原定义的exchange、queue、topic全部清空
     		//因此，需要每次异常出现并中断后，重新进行这些定义。各队列定义各队列的
     		//queueInit();//定义所有该MQCLient相关的队列和topic
     		if(connection==null)
     			connection = queueList.getConnection();
     		else{
   				try{
   					connection.close();
   				}catch(Exception e){
   					logger.warn("消息队列出现异常，尝试关闭消息队列又出现异常：",e);
   				}
     			connection = queueList.getConnection();
     		}	     			

	     	try {	 
	     		channel= connection.createChannel();
		    	if(channel==null){
		    	    logger.error("建立消息服务器通道失败");
		    	}else{
		    	    	logger.info("warnlistener连接消息服务器成功");
		    	    	try{
		    	    		channel.queueDeclare(warnQueueName, false, false,true, null);
		    	    		logger.warn("声明消息队列"+warnQueueName+"成功！");
		    	    	}catch(IOException eee){
		    	    		logger.warn("声明消息队列"+warnQueueName+"异常！",eee);
		    	    		channel.queueDelete(warnQueueName);
		    	    		channel.queueDeclare(warnQueueName, false, false,true, null);		
		    	    	}
    	    			logger.warn("开始声明交换"+CloudFactory.exchangename);
		    	    	channel.exchangeDeclare(CloudFactory.exchangename, "topic");//声明交换OK！
		    	    	logger.warn("声明交换"+CloudFactory.exchangename+"成功！");
					    //绑定topic
					    String topiclist=staticparameter.getValue(warnQueueName,"GJ");//获得告警队列名字
					    //String warnExchange=staticparameter.getValue("warnExchange","warnExchange");//获得告警队列名字
					    String[] topic=topiclist.split(",");
					    int topicnum=topic.length;
					    logger.info(warnQueueName+"开始绑定topic");
					    for(int i=0;i<topicnum;i++){
					    	channel.queueBind(warnQueueName,CloudFactory.exchangename, topic[i]);
						    logger.warn(warnQueueName+"绑定"+topic[i]+"成功");
			    	    }				    		

				    	QueueingConsumer consumer = new QueueingConsumer(channel);
				    	channel.basicConsume(warnQueueName, true, consumer);
				 		QueueingConsumer.Delivery delivery;				    	
				 		if(warntype==1)
				 			logger.info("MQClient warnlistener 正在等待告警信息");
				 		if(warntype==2)
				 			logger.info("MQClient warnlistener 正在等待预警信息");
				 		if(warntype==3)
				 			logger.info("MQClient warnlistener 正在等待RTA信息");
				 		if(warntype==4)
				 			logger.info("MQClient warnlistener 正在等待套牌信息");
				 		if(warntype==5)
				 			logger.info("MQClient warnlistener 正在等待旅行时间信息");
				 		//侦听告警消息队列
				    	while(true){
							//Thread.sleep(10000);
				    		if (connection==null ||!connection.isOpen()){
				    			logger.warn("消息服务器连接有问题，退出重连！");
								 break;
							}
							delivery = consumer.nextDelivery();
			
					    	String message = new String(delivery.getBody(),"UTF-8");
					    	if(message==null) logger.error("收到的message为空");
					    	else{
						    	logger.info("warnlisterner.java run(): 告警listener Received:" + message);
						    	
						    	//根据接收到的告警消息，调用相应的callback进行处理
						    	//告警处理
								if(warntype==1){//AlarmCallback
									logger.info("收到告警Alarm消息");
									//解析告警消息
									ArrayList<Alarm> alarm=new ArrayList<Alarm>();
									Alarm a=new Alarm();
									a.setAlarmTime(getValue(message,"Alarmtime","=",";"));
									a.setLayoutId(getValue(message,"BKBH","=",";"));
									if(a.getLayoutId()==null||a.getLayoutId()=="") ;
									else{
										a.setResultCar(getResultCar(message));
										String[] urls=a.getResultCar().getImgUrls();
										logger.debug("urls.length="+urls.length);
										logger.debug(a.getResultCar().getPlateNumber());
										if(urls[0]==null||urls[0]=="")
											logger.debug("url is null");
										else
											logger.debug(urls[0]);
										alarm.add(a);
										
										//报警等回调函数
										if(CloudFactory.ac!=null){
											CloudFactory.ac.setAlarmInfo(alarm);
											logger.info("AlarmCallback execute OK!");
										}else
											logger.error("AlarmCallback instance is null");
									}
								}
								//预警处理
								if(warntype==2){//RTACallback.setWarnInfo
									logger.info("MQClient 收到RTA预警yj消息");
									//解析预警消息
									ResultCar car=new ResultCar();
									car=getResultCar(message);
									HashMap<Long,HashSet<String>> map=new HashMap<Long,HashSet<String>>();
									String sid=getValue(message,"sessionid",":",";");
									if(sid==""||sid==null);
									else{
									long sessionid;
										sessionid=Long.parseLong(sid);
										//预警类型
										String yjstr=getValue(message,"warntype",":",";");
										String[] yjlist=yjstr.split(","); 
										HashSet<String> yj=new HashSet<String>();
										for(int i=0;i<yjlist.length;i++)
											yj.add(yjlist[i]);	
										map.put(sessionid, yj);					
										
										//预警等回调函数
										if(CloudFactory.rc!=null){
											if(CloudFactory.rc.setWarnInfo(car, map))
												logger.info("RTAcallback instance setWarnInfo return TRUE!");
											else
												logger.warn("RTAcallback instance setWarnInfo return FALSE!");
										}
										else
											logger.error("RTAcallback instance is null");
									}
								}
								//RTA处理
								if(warntype==3){//RTACallback.setRTAInfo，卡口过车和特殊车辆
									logger.info("MQClient 收到RTA监控monite消息");
									//解析RTA消息
									ResultCar car=new ResultCar();
									QueryInfo queryInfo=new QueryInfo();
									car=getResultCar(message);
									String sid=getValue(message,"sessionid",":",";");
									if(sid==""||sid==null);
									else{
										long sessionid;
										sessionid=Long.parseLong(sid);
										queryInfo.setSessionId(sessionid);
										
										//预警等回调函数
										if(CloudFactory.rc!=null){
											if(CloudFactory.rc.setRTAInfo(car, queryInfo))
												logger.info("RTAcallback instance setRTAInfo return TRUE!");
											else
												logger.info("RTAcallback instance setRTAInfo return FALSE!");
										}
										else
											logger.info("RTACallback instance is null");
									}
								}
								//套牌处理
								if(warntype==4){//DuplicateCallback
									logger.info("MQClient 收到套牌消息");
									//解析告警消息
									ArrayList<DuplicateCar> duplicatecar=new ArrayList<DuplicateCar>();
									DuplicateCar a=new DuplicateCar();
									a.setFirstImgUrl(getValue(message,"FirstImgUrl","=",";"));
									if(a.getFirstImgUrl()=="");
									else{
										a.setFirstTgs(getValue(message,"FirstTgs","=",";"));
										a.setFirstTime(getValue(message,"FirstTime","=",";"));
										a.setIntervalTime(getValue(message,"IntervalTime","=",";"));
										a.setPlateNumber(getValue(message,"PlateNumber","=",";"));
										a.setPlateType(getValue(message,"PlateType","=",";"));
										a.setRank(getValue(message,"Rank","=",";"));
										a.setSecondImgUrl(getValue(message,"SecondImgUrl","=",";"));
										a.setSecondTgs(getValue(message,"SecondTgs","=",";"));
										a.setSecondTime(getValue(message,"SecondTime","=",";"));
										//System.out.println(a.getFirstImgUrl());
										duplicatecar.add(a);
										//System.out.println(duplicatecar.size());
										//System.out.println(duplicatecar.get(0).getSecondImgUrl());
										logger.debug("第一条过车记录的图片访问地址："+a.getFirstImgUrl());
										logger.debug("第一条过车记录的图片访问地址："+a.getSecondImgUrl());
										//logger.debug(duplicatecar.get(0).getSecondImgUrl());
										
										//报警等回调函数
										if(CloudFactory.tpc!=null){
											CloudFactory.tpc.setDuplicateCarInfo(duplicatecar);
											logger.info("DuplicateCallback execute OK!");
										}else
											logger.info("DuplicateCallback instance is null");
									}
								}
								if(warntype==5){//TravelTimeCallback
									logger.info("MQClient 收到旅行时间消息");
									//解析旅行时间消息
									ArrayList<TravelTimeRecord> ttrs = new ArrayList<TravelTimeRecord>();
									String[] messageinfo = message.split("#@");
									String[] submessage = messageinfo[0].split("@#");
									String num = messageinfo[1];
									for(int i=0; i<Integer.parseInt(num); i++)
									{
										String linkId=getValue(submessage[i],"linkId","=",";");
					                    String linkName=getValue(submessage[i],"linkName","=",";");
					                    if(linkId.equals("")){
					                    	logger.info("Receive illegal data");
					                    } else {
						                    int linkLength=getIntValue(submessage[i],"linkLength","=",";");
						                    int normalTravelTime=getIntValue(submessage[i],"normalTravelTime","=",";");
						                    int actualTravelTime=getIntValue(submessage[i],"actualTravelTime","=",";");
						                    int delayedTime=getIntValue(submessage[i],"delayedTime","=",";");
						                    double avgTravelSpeed=Double.parseDouble(getValue(submessage[i],"avgTravelSpeed","=",";"));
						                    java.lang.String updateTime=getValue(submessage[i],"updateTime","=",";");
						                    TravelTimeRecord ttr=new TravelTimeRecord(linkId,linkName,linkLength,normalTravelTime,actualTravelTime,delayedTime,avgTravelSpeed,updateTime);
						                    ttrs.add(ttr);
					                    }
									}
									TravelTimeRecord[] ttra= new TravelTimeRecord[ttrs.size()];
									ttra = ttrs.toArray(ttra);
									ttrs.clear();
									//旅行时间等回调函数
									if(CloudFactory.ttc!=null){
										CloudFactory.ttc.addTravelTimeEstimationRecords(ttra);
										logger.info("TravelTimeCallback execute OK!");
									}else
										logger.error("TravelTimeCallback instance is null");
								}
					    	}
		    	    }
	    		}//第一层if...else...
			}catch(Exception e){
				try{
					channel.close();
					connection.close();
				}catch(Exception ee){
					logger.error("warllistener运行过程捕捉到消息服务异常,关闭消息链接是出现异常:",ee);
				}
				logger.error("warllistener运行过程捕捉到消息服务异常:",e);
			}//try...catch...
	     	try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}//第一层while
    }
    
    public ResultCar getResultCar(String str){
		ResultCar car=new ResultCar();

		car.setCaptureDir(getValue(str,"CAPTUREDIR","=",";"));
		car.setCarBrand(getValue(str,"CARBRAND","=",";"));
		car.setCarColor(getValue(str,"CARCOLOR","=",";"));
		car.setCarState(getValue(str,"CARSTATE","=",";"));
		car.setContent(getValue(str,"warntype","=",";"));//设置违法类型
		car.setLandIe(getValue(str,"DRIVEWAY","=",";"));
		car.setLoncatonId(getValue(str,"LOCATIONID","=",";"));
		//car.setMainCar(getValue(str,"CAPTUREDIR","=",";"));//是否为主车,伴随车辆使用
		car.setPlateColor(getValue(str,"PLATECOLOR","=",";"));
		car.setPlateNumber(getValue(str,"CARPLATE","=",";"));
		car.setPlateType(getValue(str,"PLATETYPE","=",";"));
		car.setSpeed(getValue(str,"SPEED","=",";"));
		car.setTgs(getValue(str,"TGSID","=",";"));
		car.setTimeStamp(getValue(str,"PASSTIME","=",";"));
		car.setTravelOrientation(getIntValue(str,"DRIVEDIR","=",";"));
		//图片是一个数组，所以要单独处理
		String value=getValue(str,"url","=",";");
		String[] strlist={""};
		strlist[0]=value;
		car.setImgUrls(strlist);
		return car;
    }
    
	/**在msg中获取tag的value，例如 TIME=2012;TGS=1,2,3;NAME=yhy,tag为"NAME",tag1为"=",tag2为";"
	 * @param msg：需要进行处理的字符串
	 * @param tag：需要获取value的tag值
	 * @param tag0：value中的name和value的分割符
	 * @param tagl：不同keyvalue之间的分隔符，例如工控机的就是换行符
	 * @return ：返回tag的value
	 */
    public String getValue(String msg,String tag,String tag1,String tag2){

        int pos0=msg.indexOf(tag);
        if(pos0<0) {
        	logger.info(tag+" doesn't exist!");
        	return "";
        }
        int pos1=msg.indexOf(tag1,pos0);
        int pos2=msg.indexOf(tag2,pos0);
        String value;
        if(pos2<0) {     
        	//System.out.println("tag is at the tail!");
        	pos2=msg.length();
			value=msg.substring(pos1+1,pos2);//value
        }else{
        	//System.out.println("tag is at the middle!");
        	if(pos1+1>pos2)
        		return "";
			value=msg.substring(pos1+1,pos2);//value
        }
		return value;		
	}
        
    public int getIntValue(String msg,String tag,String tag1,String tag2){

        int pos0=msg.indexOf(tag);
        if(pos0<0) {
        	logger.info(tag+" doesn't exist!");
        	return 0;
        }
        int pos1=msg.indexOf(tag1,pos0);
        int pos2=msg.indexOf(tag2,pos0);
        String value;
        if(pos2<0) {     
        	//System.out.println("tag is at the tail!");
        	pos2=msg.length();
			value=msg.substring(pos1+1,pos2);//value
        }else{
        	//System.out.println("tag is at the middle!");
        	if(pos1+1>pos2)
        		return 0;
			value=msg.substring(pos1+1,pos2);//value
        }
		if(value==null)
			return 0;
		else
			return Integer.parseInt(value);		
	}    	    
}
