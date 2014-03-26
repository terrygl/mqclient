package com.hualu.cloud.clientinterface.realtime;

import java.util.ArrayList;

import org.apache.log4j.Logger;

import com.ehl.itgs.interfaces.LayoutInterface;
import com.ehl.itgs.interfaces.bean.Layout;
import com.ehl.itgs.interfaces.bean.QueryInfo;
import com.google.gson.Gson;
import com.hualu.cloud.basebase.staticparameter;
import com.hualu.cloud.clientinterface.CloudFactory;

/**
 * 
 *  布控（公开性布控及私密性布控）、违法未处理 接口
 */
public class InterfaceLayout implements LayoutInterface{
	public static Logger logger = Logger.getLogger(InterfaceLayout.class);
	/**
	 * 
	 *  布控：key分为HPW前缀表示按照号牌号码和号牌类型比对告警，TSGW前缀表示按照卡口
	 * @param bean 布控条件集合
	 * @return true：成功,false:失败
	 */
	public boolean layout(ArrayList<Layout> bean,QueryInfo queryInfo) throws Exception {
		if(bean.size()<1) return false;
		for(int i=0;i<bean.size();i++){
			Layout a=bean.get(i);
			if(a.getLayoutId()==null||a.getLayoutId().length()<1)
				return false;
			if(a.getPlateNumber()==null||a.getPlateNumber().length()<1)
				return false;
			if(a.getPlateType()==null||a.getPlateType().length()<1)
				return false;
			if(a.getTgs()==null||a.getTgs().length<1) return false;
			for(int j=0;j<a.getTgs().length;j++){
				if(a.getTgs()[j]==null||a.getTgs()[j].length()<1)
					return false;
			}			
		}
	
		//定义协议名称
		String MSG1="InterfaceLayout.Layout";	
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
		
		logger.info("启动线程发送Layout命令消息："+MSG);
		return ch.call();
		
	}
	
	
	/**
	 * 
	 *  撤控
	 * @param bean 布控条件集合
	 * @return true：成功,false:失败
	 */
	public boolean cancelLayout(ArrayList<Layout> bean,QueryInfo queryInfo) throws Exception{
		if(bean.size()<1) return false;
		for(int i=0;i<bean.size();i++){
			Layout a=bean.get(i);
			if(a.getLayoutId()==null||a.getLayoutId().length()<1)
				return false;
			if(a.getPlateNumber()==null||a.getPlateNumber().length()<1)
				return false;
			if(a.getPlateType()==null||a.getPlateType().length()<1)
				return false;
			if(a.getTgs().length<1) return false;
			for(int j=0;j<a.getTgs().length;j++){
				if(a.getTgs()[j]==null||a.getTgs()[j].length()<1)
					return false;
			}			
		}
		
		//定义协议名称
		String MSG1="InterfaceLayout.cancelLayout";	
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
		
		logger.info("启动线程发送cancelLayout命令消息："+MSG);
		return ch.call();

	}
}
