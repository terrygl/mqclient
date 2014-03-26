package com.hualu.cloud.clientinterface;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.HTablePool;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.log4j.Logger;

import com.ehl.itgs.interfaces.LayoutInterface;
import com.ehl.itgs.interfaces.QueryInterface;
import com.ehl.itgs.interfaces.RTAInterface;
import com.ehl.itgs.interfaces.TravelTimeInterface;
import com.ehl.itgs.interfaces.bean.traveltime.LinkConfig;
import com.ehl.itgs.interfaces.callback.AlarmCallback;
import com.ehl.itgs.interfaces.callback.DuplicateCarCallback;
import com.ehl.itgs.interfaces.callback.LayoutCallback;
import com.ehl.itgs.interfaces.callback.RTACallback;
import com.ehl.itgs.interfaces.callback.ServerStatusCallBack;
import com.ehl.itgs.interfaces.callback.TravelTimeCallback;
import com.ehl.itgs.interfaces.factory.FactoryInterface;
import com.hualu.cloud.basebase.loglog;
import com.hualu.cloud.basebase.staticparameter;
import com.hualu.cloud.clientinterface.offline.CloudQuery;
import com.hualu.cloud.clientinterface.offline.CloudTravelTime;
import com.hualu.cloud.clientinterface.realtime.InterfaceLayout;
import com.hualu.cloud.clientinterface.realtime.InterfaceRTA;
import com.hualu.cloud.clientinterface.realtime.queueList;

public class CloudFactory implements FactoryInterface{
	static String process=loglog.setProcessname("MQClient");
	public static RTACallback rc;
	public static AlarmCallback ac;
	public static DuplicateCarCallback tpc;
	public static TravelTimeCallback ttc;
	public static LayoutCallback loc;
	public static ServerStatusCallBack ssc;
	public static Logger logger = Logger.getLogger(CloudFactory.class);
	public static boolean addAlarm=false; 
	public static String msgsplit=staticparameter.getValue("msgsplit", "@@");
	public static String parametersplit=staticparameter.getValue("parametersplit", "##");
    public static int responseTimeout=staticparameter.getIntValue("responseTimeout",60000);//60s为超时默认
    public static int waitResponseSleep=staticparameter.getIntValue("waitResponseSleep",100);//100ms为默认
	public static ArrayList<String> linkcfgs = new ArrayList<String>();
	static int frequenceTime = staticparameter.getIntValue("frequenceTime",10); //返回数据时间间隔，单位s
	public static String exchangename=staticparameter.getValue("warnExchange", "warnExchange");
	static int timerfrequency = staticparameter.getIntValue("timerfrequency",20); //心跳检测时间间隔
	static int update2timertimes = staticparameter.getIntValue("update2timertimes",3); //更新线路信息间隔比心跳检测时间间隔
	static int i = 1;

	/**
	 * 创建监听
	 */
	public void addListener(Object object){
		//loglog.processname="MQClient";
		Class<? extends Object> a=object.getClass();
		Class<RTACallback> a1=com.ehl.itgs.interfaces.callback.RTACallback.class;
		Class<AlarmCallback> a2=com.ehl.itgs.interfaces.callback.AlarmCallback.class;
		Class<DuplicateCarCallback> a3=com.ehl.itgs.interfaces.callback.DuplicateCarCallback.class;
		Class<TravelTimeCallback> a4=com.ehl.itgs.interfaces.callback.TravelTimeCallback.class;
		Class<LayoutCallback> a5=com.ehl.itgs.interfaces.callback.LayoutCallback.class;
		Class<ServerStatusCallBack> a6=com.ehl.itgs.interfaces.callback.ServerStatusCallBack.class;

		if(a2.isAssignableFrom(a))
			addAlarm=true;
		else
			addAlarm=false;
		
		if(queueList.initstate==0){
			logger.info("正在进行接口初始化");
			try {
				//静态参数初始化
				//staticparameter.staticparameterinit();
				//初始化命令消息队列
				queueList.listinit(addAlarm);
				logger.info("完成接口初始化");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				logger.fatal("Client CloudFactory.java addMSG()执行异常");
				e.printStackTrace();
			}
		}
		logger.info("addListener:"+object.getClass().getName());
			
		if(a1.isAssignableFrom(a)){
			logger.info("get RTACallback instance");
			rc=(RTACallback) object;
			logger.info(rc.getClass().getName());
		}
		else if(a2.isAssignableFrom(a)){
			logger.info("get AlarmCallback instance");
			ac=(AlarmCallback) object;
			logger.info(ac.getClass().getName());
		}
		else if(a3.isAssignableFrom(a)){
			logger.info("get DuplicateCarCallback instance");
			tpc=(DuplicateCarCallback) object;
			logger.info(tpc.getClass().getName());
		}
		else if(a4.isAssignableFrom(a)){
			logger.info("get TravelTimeCallback instance");
			ttc=(TravelTimeCallback) object;
			logger.info(ttc.getClass().getName());	
			Timer timer = new Timer();
		    timer.schedule(new TravelTimeTask(), 0, update2timertimes*timerfrequency*1000); //frequenceTime秒调用一次run()
		}
		else if(a5.isAssignableFrom(a)){
			logger.info("get LayoutCallback instance");
			loc=(LayoutCallback) object;
			logger.info(loc.getClass().getName());
		}
		else if(a6.isAssignableFrom(a)){
			logger.info("get ServerStatusCallback instance");
			ssc=(ServerStatusCallBack) object;
			logger.info(ssc.getClass().getName());
		}
		else{
			logger.info("请核查，没有匹配的 callback接口:"+object.getClass().getName());
		}		
	logger.info("end of addListern");
}
	
	/**
	 * 提交监听，曾为山大提供支持
	 * 初始化消息队列：命令消息队列+告警接受消息队列
	 */
	@Deprecated
	public boolean commitListener(){
		if(addAlarm==true){
			if(ac==null)
				logger.warn("AlarmCallback is null,please check!");
			else
				logger.info("AlarmCallback is OK");
			
		}else{
			if(rc==null)
				logger.warn("RTACallback is null,please check!");
			else
				logger.info("RTACallback is OK");
			if(tpc==null)
				logger.warn("DuplicateCarCallback is null,please check!");
			else
				logger.info("DuplicateCarCallback is OK");
			if(ttc==null)
				logger.warn("TravelTimeCallback is null,please check!");
			else
				logger.info("TravelTimeCallback is OK");
			if(loc==null)
				logger.warn("LayoutCallback is null,please check!");
			else
				logger.info("LayoutCallback is OK");
			if(ssc==null)
				logger.warn("ServerStatusCallBack is null,please check!");
			else
				logger.info("ServerCallBack is OK");			
		}
		return true;
	}
	

	/**
	 * 实现查询接口，返回实现类
	 */
	public  QueryInterface createQuery(){
		logger.info("创建查询接口实例");
		CloudQuery cloudquery=new CloudQuery();
		return cloudquery;
	}
	/**
	 * 实现实时监控、预警接口，返回实现类
	 */
	public  RTAInterface  createRTA(){
		logger.info("创建预警监控接口实例");
		InterfaceRTA cloudrta=new InterfaceRTA();
		return cloudrta;
	}
	/**
	 * 实现布控接口，返回实现类
	 */
	public  LayoutInterface  createLayout(){
		logger.info("创建布控接口实例");
		InterfaceLayout cloudlayout=new InterfaceLayout();
		return cloudlayout;
	}
	/**
	 * 实现旅行时间分析接口，返回实现类
	 */
	public   TravelTimeInterface createTravelTime(){
		logger.info("创建旅行时间接口实例");
		CloudTravelTime cloudtraveltime=new CloudTravelTime();
		return cloudtraveltime;
	}
	
	/**
	 * 更新旅行时间线路信息
	 */
	public static void doUpdateLinkConfig(){
		logger.info("update link configs");
		LinkConfig[] list = CloudFactory.ttc.getAllLinkConfigs();
		//采集所有连线信息，存入list
		for(LinkConfig lc:list) {
			String linkId = lc.getLinkId(); //连线编号
			String linkName = lc.getLinkName();//连线名称
			int linkLength = lc.getLinkLength();//连线长度，单位：米
			int normalTravelTime = lc.getNormalTravelTime();//标准通过时间, 单位：秒
			double maxSpeed = lc.getMaxSpeed();//限速，单位km/h
			String startTgsId = lc.getStartTgsId();//起始卡口编号
			String startTgsDirectionId = lc.getStartTgsDirectionId();//起始卡口方向
			String endTgsId = lc.getEndTgsId();//结束卡口编号
			String endTgsDirectionId = lc.getEndTgsDirectionId();//结束卡口方向
						
			String linkconfigrowkey = linkId+","+startTgsId+","+endTgsId;  //linkconfig表的rowkey=连线ID+起始卡口+终止卡口
			String linkconfigvalue = linkId+","+linkName+","+startTgsId+","+endTgsId+","+startTgsDirectionId+","+
					endTgsDirectionId+","+maxSpeed+","+linkLength+","+normalTravelTime;

			if(!(linkId+"").equals("") && !(startTgsId.equals("")) && !(endTgsId.equals("")) && !(startTgsId.equals(endTgsId))) {
				try {
					addRecord ("by_linkconfig", linkconfigrowkey, "cf", "info", linkconfigvalue);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} 
    } 
	
	 /** 
     * 插入一条记录（cell） 
     * @param tableName:表名
     * @param rowKey:rowkey
     * @param family:列族
     * @param qualifier:标示符
     * @param value:值
     */   
	public static void addRecord (String tableName, String rowKey, String family, String qualifier, String value)  
            throws Exception {     	
		HTableInterface table = null; 
		try {
			table = hTablePool.getTable(Bytes.toBytes(tableName));
	        Put put = new Put(Bytes.toBytes(rowKey));  
	        put.add(Bytes.toBytes(family),Bytes.toBytes(qualifier),Bytes.toBytes(value));  
	        table.put(put);  
	        System.out.println("insert recored " + rowKey + " to table " + tableName +" ok.");  
        } catch (IOException e){  
			e.printStackTrace();  
		} finally {  
			if (table != null){  
				table.close();  
            }  
		}   
	}
	
	private static Configuration conf = null;
	private static HTablePool hTablePool = null; 		
	static String maxclockskew = staticparameter.getValue("maxclockskew","18000000");
	static String rootdir = staticparameter.getValue("rootdir","hdfs://host61:9000/hbase");
	static String distributed = staticparameter.getValue("distributed","true");
	static String clientPort = staticparameter.getValue("clientPort","2181");
	static String zookeeperquorum = staticparameter.getValue("zookeeperquorum","host51,host52,host53");
	static String regionclasses = staticparameter.getValue("regionclasses","com.ehl.dataselect.business.processor.sync.abnormalBehaviorAnalysis.AbnormalBehaviorEndpoint");
	static String master = staticparameter.getValue("master","host59:60000");
	
    /** 
     * 初始化配置 
     */    
    static {
    	conf = HBaseConfiguration.create();
    	conf.set("hbase.master.maxclockskew", maxclockskew);
    	conf.set("hbase.rootdir", rootdir);
    	conf.set("hbase.cluster.distributed", distributed);
    	conf.set("hbase.zookeeper.property.clientPort", clientPort);   
    	conf.set("hbase.zookeeper.quorum", zookeeperquorum); 
    	conf.set("hbase.coprocessor.user.region.classes", regionclasses);
    	conf.set("hbase.master", master); 
       	hTablePool = new HTablePool(conf, 1024); 
    } 
    
	class TravelTimeTask extends TimerTask {  
		public void run() {
			logger.info("update link configs");
			LinkConfig[] list = CloudFactory.ttc.getAllLinkConfigs();
			//采集所有连线信息，存入list
			for(LinkConfig lc:list) {
				String linkId = lc.getLinkId(); //连线编号
				String linkName = lc.getLinkName();//连线名称
				int linkLength = lc.getLinkLength();//连线长度，单位：米
				int normalTravelTime = lc.getNormalTravelTime();//标准通过时间, 单位：秒
				double maxSpeed = lc.getMaxSpeed();//限速，单位km/h
				String startTgsId = lc.getStartTgsId();//起始卡口编号
				String startTgsDirectionId = lc.getStartTgsDirectionId();//起始卡口方向
				String endTgsId = lc.getEndTgsId();//结束卡口编号
				String endTgsDirectionId = lc.getEndTgsDirectionId();//结束卡口方向
							
				String linkconfigrowkey = linkId+","+startTgsId+","+endTgsId;  //linkconfig表的rowkey=连线ID+起始卡口+终止卡口
				String linkconfigvalue = linkId+","+linkName+","+startTgsId+","+endTgsId+","+startTgsDirectionId+","+
						endTgsDirectionId+","+maxSpeed+","+linkLength+","+normalTravelTime;

				if(!(linkId+"").equals("") && !(startTgsId.equals("")) && !(endTgsId.equals("")) && !(startTgsId.equals(endTgsId))) {
					try {
						addRecord ("by_linkconfig", linkconfigrowkey, "cf", "info", linkconfigvalue);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		} 
    } 
	
}
