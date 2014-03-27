package com.hualu.cloud.clientinterface.realtime;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import com.ehl.itgs.interfaces.RTAInterface;
import com.ehl.itgs.interfaces.bean.CarQueryBean;
import com.ehl.itgs.interfaces.bean.QueryInfo;
import com.ehl.itgs.interfaces.bean.RTABean;
import com.ehl.itgs.interfaces.bean.TgsQueryBean;
import com.ehl.itgs.interfaces.bean.WarnQueryBean;
import com.google.gson.Gson;
import com.hualu.cloud.clientinterface.baseop;
import com.hualu.cloud.db.memoryDB;
import com.hualu.cloud.clientinterface.CloudFactory;

public class InterfaceRTA implements RTAInterface{
	public static Logger logger = Logger.getLogger(InterfaceRTA.class);
	/**
	 * Description: 卡口通行车辆实时监控
	 * @param bean 监控参数
	 * @param queryInfo 请求信息
	 * @return true：成功,false:失败
	 * @throws Exception
	 */
	public boolean addTgs(RTABean bean,QueryInfo queryInfo) throws Exception{
		if(bean.getTgs()==null||bean.getTgs().length()<1) return false;
		if(queryInfo.getSessionId()==null) return false;
		//System.out.println("进入addTgs方法");
		//定义协议名称
		String MSG1="InterfaceRTA.addTgs";	
		//准备参数的文本串
		Gson gson=new Gson();
		//获得方法第一个参数的字符串内容(List格式)
		String MSG2=gson.toJson(bean);
		//获得方法第二个参数的字符串内容
		String MSG3=gson.toJson(queryInfo);
		
		//组织消息格式
		String MSG=MSG1+CloudFactory.msgsplit+MSG2+CloudFactory.parametersplit+MSG3;
		
		//启动消息命令线程处理(用回调函数可实现有返回值的线程)
		commander  ch= new commander(MSG); 
		/*
		if(MSG2==null)
			System.out.println("String MSG2=gson.toJson(bean) is null" );
		else
			System.out.println(MSG2);
		if(MSG3==null)
			System.out.println("String MSG3=gson.toJson(queryInfo) is null" );
		else
			System.out.println(MSG3);
		*/
		
		logger.info("启动线程发送addTgs命令消息："+MSG);
		return ch.call();
		
	}

	/**
	 *  特定车辆监控
	 * @param plateType 号牌类型
	 * @param plateNumber 号牌号码
	 * @param queryInfo 请求信息
	 * @return true：成功,false:失败
	 */
	public boolean addCar(String plateType,String plateNumber,QueryInfo queryInfo) throws Exception{
		if(plateType==null||plateType.length()<1) return false;
		if(plateNumber==null||plateNumber.length()<1) return false;
		if(queryInfo.getSessionId()==null) return false;
		//定义协议名称
		String MSG1="InterfaceRTA.addCar";	
		//准备参数的文本串
		Gson gson=new Gson();
		//获得方法第一个参数的字符串内容(List格式)
		String MSG2=gson.toJson(plateType);
		//获得方法第二个参数的字符串内容
		String MSG3=gson.toJson(plateNumber);
		//获得方法第三个参数的字符串内容
		String MSG4=gson.toJson(queryInfo);
		
		//组织消息格式
		String MSG=MSG1+CloudFactory.msgsplit+MSG2+CloudFactory.parametersplit+MSG3+CloudFactory.parametersplit+MSG4;
		
		//启动消息命令线程处理(用回调函数可实现有返回值的线程)
		commander  ch= new commander(MSG); 
		
		logger.info("启动线程发送addCar命令消息："+MSG);
		return ch.call();
	}
	
	/**
	 * Description: 预警
	 * @param warnType 预警类型
	 * @param bean 预警参数
	 * @param queryInfo 
	 * @return true：成功,false:失败
	 * @throws Exception
	 */
	public boolean addWarn(String[] warnType,RTABean[] bean,QueryInfo queryInfo) throws Exception{
		if(queryInfo.getSessionId()==null) return false;
		if(warnType==null||warnType.length<1) return false;
		if(queryInfo.getSessionId()==null) return false;
		//定义协议名称
		String MSG1="InterfaceRTA.addWarn";	
		//准备参数的文本串
		Gson gson=new Gson();
		//获得方法第一个参数的字符串内容(List格式)
		String MSG2=gson.toJson(warnType);
		//获得方法第二个参数的字符串内容
		String MSG3=gson.toJson(bean);
		//获得方法第三个参数的字符串内容
		String MSG4=gson.toJson(queryInfo);
		
		//组织消息格式
		String MSG=MSG1+CloudFactory.msgsplit+MSG2+CloudFactory.parametersplit+MSG3+CloudFactory.parametersplit+MSG4;
		
		//启动消息命令线程处理(用回调函数可实现有返回值的线程)
		commander  ch= new commander(MSG); 
		
		logger.info("启动线程发送addWarn命令消息："+MSG);
		return ch.call();
	}
	
	/**
	 *  取消
	 * @param queryInfo 请求信息
	 * @return true：成功,false:失败
	 */
	public boolean cancel(QueryInfo queryInfo) throws Exception{
		if(queryInfo.getSessionId()==null) return false;
		//定义协议名称
		String MSG1="InterfaceRTA.cancel";	
		//准备参数的文本串
		Gson gson=new Gson();
		//获得方法第一个参数的字符串内容
		String MSG2=gson.toJson(queryInfo);
		
		//组织消息格式
		String MSG=MSG1+CloudFactory.msgsplit+MSG2;
		
		//启动消息命令线程处理(用回调函数可实现有返回值的线程)
		commander  ch= new commander(MSG); 
		
		logger.info("启动线程发送cancel命令消息："+MSG);
		return ch.call();
	}
	
	 /**
	 *  查询已监控的特定车辆：将内存数据库中的信息解析后返给前端
	 */
	 public List<CarQueryBean>	queryCar(){
		 //解析value，形成CarQueryBean
		 //key1=ts:plateTypeplayteNumber
		 //value1=sessionid:SessionId1,SessionId2...
		 //key2=ts:sessiondid
		 //value2=ts:plateTypeplayteNumber
		 
		 List<CarQueryBean> lcqb=new ArrayList<CarQueryBean>();
		 memoryDB m=new memoryDB();
		 String plateType="";
		 String plateNumber="";
		 //获取前缀为"ts:"的数据
		 Set<String> str=m.memorydb.keys("ts:*");
		 if(str==null||str.size()<1) return null;
		 String key,value;
		 Iterator<String> it=str.iterator();
		 
		 //逐条处理将反解析的结果加入lcqb。
		 while(it.hasNext()){		 	
			 //解析key，获得车牌类型和车牌号码
			 key=it.next();
			 value=m.get(key);
			 if(value.indexOf("sessionid:")>=0){
				 //只有值中含有sessionid的记录才需要反解析
				 String[] s1=key.split(":");
				 if(s1.length>1){
					 logger.debug(s1[1]);
					 char c=s1[1].charAt(1);
					 if(baseop.isNumber(c)){//车牌类型为两位数
						 plateType=s1[1].substring(0, 2);
						 plateNumber=s1[1].substring(2);
					 }else{//车牌类型为一位数字
						 plateType=s1[1].substring(0, 1);
						 plateNumber=s1[1].substring(1);
					 }
				 }
				 
				 //解析value，获得sessionid
				 if(value!=null){
					 String[] s2=value.split(":");
					 String[] ss2=s2[1].split(",");
					 for(int i=0;i<ss2.length;i++){
						 CarQueryBean cqb=new CarQueryBean();
						 QueryInfo queryinfo=new QueryInfo();
						 queryinfo.setSessionId(Long.parseLong(ss2[i]));
						 //设置platetype和platenumber
						 cqb.setPlateNumber(plateNumber);
						 cqb.setPlateType(plateType);
						 //设置sessionid
						 cqb.setQueryInfo(queryinfo);
						 lcqb.add(cqb);
					 }
					 s2=null;
					 ss2=null;
				 }
			 }
		 }
		 
		 //退出前清除memorydb
		 m.finalize();
		 str.clear();
		 try{
			 it.remove();
		 }catch(Exception e){
			 System.out.println(e.getMessage());
		 }
		 str=null;
		 return lcqb;
	 }
	 
	 /**
	 *  查询卡口通行车辆实时监控：将内存数据库中的信息解析后返给前端
	 */
	 public List<TgsQueryBean> queryTgs(){
		 //key1=kk:tgsid
		 //value1=cd:lanelist;fx:orientationlist;sessionid:SessionId1@@cd:lanelist;fx:orientationlist;sessionid:SessionId1...
		 //key2=kk:sessiondid
		 //value2=kk:tgsid
		 List<TgsQueryBean> ltqb=new ArrayList<TgsQueryBean>();
		 memoryDB m=new memoryDB();
		 String tgsid="";

		 //获取前缀为"kk:"的数据
		 Set<String> str=m.memorydb.keys("kk:*");
		 if(str==null||str.size()<1) return null;
		 String key,value;
		 Iterator<String> it=str.iterator();
		 
		 //逐条处理将反解析的结果加入ltqb。
		 while(it.hasNext()){
			 //解析key，得到tgsid
			 key=it.next();
			 value=m.get(key);
			 //logger.debug("卡口过车监控 "+key+"   "+value);
			 
			 if(value.indexOf("sessionid:")>=0){
				//只有值中含有sessionid的记录才需要反解析
				 String[] s1=key.split(":");
				 tgsid=s1[1];
				 //logger.debug("tgsid="+tgsid);
				 
				 //解析value，形成TgsQueryBean
				 String[] s2=value.split("@@");
				 for(int i=0;i<s2.length;i++){
					 //logger.debug(i+"  "+s2[i]);
					 String cd=baseop.getvalue(s2[i],"cd",":",";");
					 String[] lanelist=cd.split(",");
					 String fx=baseop.getvalue(s2[i],"fx",":",";");
					 String[] orientationlist=fx.split(",");
					 String sessionid=baseop.getvalue(s2[i],"sessionid",":",";");
					 TgsQueryBean tqb=new TgsQueryBean();
					 QueryInfo queryinfo=new QueryInfo();
					 queryinfo.setSessionId(Long.parseLong(sessionid));
					 //logger.debug(queryinfo.getSessionId());
					 
					 tqb.setQueryInfo(queryinfo);
					 RTABean bean=new RTABean(null,null,null);
					 bean.setTgs(tgsid);
					 //logger.debug(bean.getTgs());
					 bean.setLandIe(lanelist);
					 //logger.debug(bean.getLandIe()[0]);
					 bean.setTravelOrientation(orientationlist);
					 //logger.debug(bean.getTravelOrientation()[0]);
					 tqb.setBean(bean);
					 
					 ltqb.add(tqb);				 
				 }
			 }
		 }
		 //退出前清除memorydb
		 m.finalize();
		 str.clear();
		 //logger.debug("clearn OK!");
		 try{
			 it.remove();
		 }catch(Exception e){
			 //System.out.println(e.getMessage());
		 }
		 //logger.debug("remove OK!");
		 str=null;
		 //logger.debug("set null OK!");
		 return ltqb;
	 }

	 /**
	 *  查询所有预警信息：将内存数据库中的信息解析后返给前端
	 */
     
	 public List<WarnQueryBean>	queryWarn(){

		 //key1=yj:tgsid
		 //value1=yjtype:warnlist;cd:lanelist;fx:orientationlist;sessionid:SessionId1@@yjtype:warnlist;cd:lanelist;fx:orientationlist;sessionid:SessionId2...
		 //key2=yj:sessionid
		 //value2=tgsid1,tgsid2,.....tgsidn
		 List<WarnQueryBean> lwqb=new ArrayList<WarnQueryBean>();
		 memoryDB m=new memoryDB();
		 String tgsid,sessionid;

		 //获取前缀为"kk:"的数据
		 Set<String> str=m.memorydb.keys("yj:*");
		 if(str==null||str.size()<1) return null;
		 String key,value;
		 Iterator<String> it=str.iterator();

		 //逐条处理将反解析的结果加入lwqb。
		 while(it.hasNext()){
			//解析key，得到tgsid
			 key=it.next();
			 value=m.get(key);
			 //logger.debug("最外层while循环  "+key+"   "+value);
			 if(value.indexOf("sessionid")<0){
				 //logger.info(key+"   "+value);
				 //值中没含sessionid的记录中记录着所有的tgsid,该key中包含sessionid信息
				 String[] tt=key.split(":");
				 sessionid=tt[1];
				 //value值是tgs的列表
				 String[] tgslist=value.split(",");
				 String yjtype="";
				 String cd="";
				 String fx="";
				 WarnQueryBean wqb=new WarnQueryBean();
				 QueryInfo queryinfo=new QueryInfo();
				 List<RTABean> rtabl=new ArrayList<RTABean>();
				 for(int i=0;i<tgslist.length;i++){
					 //按tgsid获取具体的预警配置
					 tgsid=tgslist[i];
					 value=m.get("yj:"+tgsid);
					 //按照yj:tgsid获取value，比较是否含有sessionid，并组织RTABean		
					 String[] s2=value.split("@@");

					 RTABean rta=new RTABean(null,null,null);
					 for(int j=0;j<s2.length;j++){
						 if(s2[j].indexOf("sessionid:"+sessionid)>=0){
							 //含有该sessionid的子串才是需要的
							 yjtype=baseop.getvalue(s2[j],"yjtype",":",";");
							 cd=baseop.getvalue(s2[j],"cd",":",";");
							 fx=baseop.getvalue(s2[j],"fx",":",";");
						 }	
					 }
					 //组成rta
					 rta.setTgs(tgsid);
					 String[] lanelist=cd.split(",");
					 rta.setLandIe(lanelist);
					 String[] orientationlist=fx.split(",");
					 rta.setTravelOrientation(orientationlist);	
					 rtabl.add(rta);			 
				 }				 
				 wqb.setBean(rtabl.toArray(new RTABean[rtabl.size()]));		
				 String[] warnlist=yjtype.split(",");
				 wqb.setWarnType(warnlist);
				 queryinfo.setSessionId(Long.parseLong(sessionid));
				 wqb.setQueryInfo(queryinfo);
				 lwqb.add(wqb);	
			 }

		 }
		 //退出前清除memorydb
		 logger.debug(lwqb.size());
		 m.finalize();
		 str.clear();
		 try{
			 it.remove();
		 }catch(Exception e){
			 System.out.println(e.getMessage());
		 }
		 str=null;
		 return lwqb;
	 }     
}
