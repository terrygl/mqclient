package com.hualu.cloud.clientinterface.offline;

import java.util.ArrayList;

import com.ehl.itgs.interfaces.QueryInterface;
import com.ehl.itgs.interfaces.bean.BackResult;
import com.ehl.itgs.interfaces.bean.QueryCar;
import com.ehl.itgs.interfaces.bean.QueryInfo;
import com.ehl.itgs.interfaces.bean.ResultCar;
import com.ehl.itgs.interfaces.bean.analysis.AbnormalBehavior;
import com.ehl.itgs.interfaces.bean.analysis.AbnormalBehaviorResult;
import com.ehl.itgs.interfaces.bean.analysis.CarPatternInfo;
import com.ehl.itgs.interfaces.bean.analysis.CarPatternResult;
import com.ehl.itgs.interfaces.bean.analysis.FollowCar;
import com.ehl.itgs.interfaces.bean.analysis.LocusCarResult;
import com.ehl.itgs.interfaces.bean.analysis.Multipoint;
import com.ehl.itgs.interfaces.bean.analysis.TheGround;
import com.ehl.itgs.interfaces.bean.analysis.TrafficFlow;
//import com.hualu.cloud.offline.process.Bayonet4;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.hualu.cloud.basebase.staticparameter;
import com.hualu.cloud.clientinterface.CloudFactory;
import com.hualu.cloud.clientinterface.realtime.commander;

public class CloudQuery implements QueryInterface{
	/**
	 * Description: 通用查询
	 * @param car 车辆信息特征
	 * @param bayonets 卡口编号列表
	 * @param begTime 通过时间<开始>
	 * @param endTime 通过时间<结束>
	 * @param begNum 分页查询开始位置
	 * @param num 分页每次查询条数
	 * @param queryInfo 查询信息（sessionId,userName,IP）
	 * @return 车辆信息集合
	 * @throws Exception
	 */
	public BackResult<ResultCar> query(QueryCar car, String[] bayonets, String begTime, String endTime,
			int begNum, int num,QueryInfo queryInfo) throws Exception{
		if(bayonets.length<1) return null;
		for(int i=0;i<bayonets.length;i++){
			if(bayonets[i].length()<1)
				return null;
		}
		//定义协议名称
		String MSG0="InterfaceQuery.query";	
		//准备参数的文本串
		Gson gson=new Gson();

		String MSG1=gson.toJson(car);
		String MSG2=gson.toJson(bayonets);
		String MSG3=gson.toJson(begTime);
		String MSG4=gson.toJson(endTime);
		String MSG5=gson.toJson(begNum);
		String MSG6=gson.toJson(num);
		String MSG7=gson.toJson(queryInfo);
		
		//组织消息格式
		String MSG=MSG0+CloudFactory.msgsplit+MSG1
				+CloudFactory.parametersplit+MSG2
				+CloudFactory.parametersplit+MSG3
				+CloudFactory.parametersplit+MSG4
				+CloudFactory.parametersplit+MSG5
				+CloudFactory.parametersplit+MSG6
				+CloudFactory.parametersplit+MSG7;
		
		//启动消息命令线程处理(用回调函数可实现有返回值的线程)
		System.out.println("CloudQuey.java query()发送命令消息："+MSG);
		commander2 ch= new commander2(MSG);

		BackResult<ResultCar> result=new BackResult<ResultCar>();
		ArrayList<ResultCar> resultcar=new ArrayList<ResultCar>();
		//response的是多条消息记录arraylist，其中每条消息的格式为：总记录数@@返回记录数@总消息数@@记录1@@记录2...
		//服务端在生成消息的时候就是按照ResultCar的格式生成的
		ArrayList<String> response=ch.call();
		
		System.out.println("CloudQuey.java query()返回结果："+response.size());
		
		//根据返回的消息构建返回值
		int l=0;
		if(response.size()==0){
			result.setTotalNum(0);			
		}
		else{
			System.out.println("CloudQueryl.java query()消息条数："+response.size());
			int len0=response.size();
			for(int i=0;i<len0;i++){
				String[] str=response.get(i).split(CloudFactory.msgsplit);
				if(i==0){
					result.setTotalNum(Integer.parseInt(str[1]));
					if((Integer.parseInt(str[0])==0)||(Integer.parseInt(str[1])==0))
						break;	
				}
				//填数据
				int j0=str.length;
				System.out.println("消息："+response.get(i));
				for(int j=3;j<j0;j++){
					System.out.println("l:"+l+"  "+str[j]);
					l++;
					ResultCar rc=gson.fromJson(str[j], new TypeToken<ResultCar>(){}.getType());;
					resultcar.add(rc);
				}
			}
			result.setBeans(resultcar);
		}
		System.out.println("消息提取记录数："+l);
		return result;
	}
	/**
	 * 
	 *  伴随查询
	 * @param crafxccar 伴随布控条件集合，主车信息中包含主车标志
	 * @param begNum 分页查询开始位置
	 * @param num 分页每次查询条数
	 * @return 键：集合<N组伴随车辆<车辆信息Bean>>，值：查询结果状态Bean 
	 */
	public BackResult<ArrayList<LocusCarResult>> getCrafxcCarInfo(FollowCar crafxccar,int begNum,int num,QueryInfo queryInfo) throws Exception{
		return null;  
	}
	/**
	 *  多点位分析比对
	 * @param multipoint 查询条件multiPoint集合
	 * @param begNum 分页查询开始位置
	 * @param num 分页每次查询条数
	 * @return 分析结果CrafxcCarResult集合
	 */
	public  BackResult<LocusCarResult>  multipointAnalysis(ArrayList<Multipoint> multipoint,int begNum,int num,QueryInfo queryInfo) throws Exception{
		return null;
	}
	
	/**
	 * 
	 *  出入案发地点分析
	 * @param theGround
	 * @param begNum 分页查询开始位置
	 * @param num 分页每次查询条数
	 * @return 键：CrafxcCarResult的结果集合，值：查询结果状态Bean
	 */
	public BackResult<LocusCarResult> passTheGround(TheGround theGround,int begNum,int num,QueryInfo queryInfo) throws Exception{
		return null;
	}
	
	/**
	 * 
	 *  行为异常车辆查询，指查询指定时间段内经过指定卡口（一个或多个）的车辆信息
	 * @param abnormalBehavior *行为异常查询条件
	 * @param begNum
	 *            分页查询开始位置
	 * @param num
	 *            分页每次查询条数
	 * @return  车辆信息集合 
	 */
	public BackResult<AbnormalBehaviorResult> getAbnormalBehaviorAnalysis(
			AbnormalBehavior abnormalBehavior, int begNum, int num,QueryInfo queryInfo) throws Exception{
		return null;
	}
	
	/**
	 * 
	 *   交通流量统计分析
	 * @param begTime 开始时间
	 * @param endTime 结束时间
	 * @param tgs 通过卡口列表
	 * @param granularity 粒度(1:1分钟,2：5分钟,3：10分钟,4:15分钟,5:半小时,6:一小时,7：一天,8:一个月,9:半年,10:一年
	 * @param baseType 归属地 见字典
	 * @param begNum 分页查询开始位置
	 * @param num 分页每次查询条数
	 * @return 车辆信息集合<不同粒度集合<不同卡口集合<流量信息>>>
	 */
	public BackResult<ArrayList<TrafficFlow>>  
		getTrafficFlow(String begTime,String endTime,String[] tgs,String baseType,String granularity, int begNum, int num,QueryInfo queryInfo)
		 throws Exception{
		return null;
	}
	
	/**
	 * 重点车辆车辆运行规律分析
	 */
	public BackResult<CarPatternResult> getPatternForCar(CarPatternInfo query, int begNum, int num,
	            QueryInfo queryInfo) throws Exception{
		return null;
	}

}
