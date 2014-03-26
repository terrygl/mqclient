package com.hualu.cloud.clientinterface.realtime;

import java.io.IOException;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;

import com.hualu.cloud.basebase.staticparameter;
import com.hualu.cloud.clientinterface.CloudFactory;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.GetResponse;
import com.rabbitmq.client.QueueingConsumer;

/**
 * 实时布控撤控的回调接口
 * 返回值就是Boolean类型
 * 
 * @author yanghy
 *
 */
public class commander implements  Callable{
    String  command = null;   
    public static Logger logger = Logger.getLogger(commander.class);
    
    public commander(String command) {   
        super();
        this.command = command;   
    }   

    @Override  
    //public Boolean call() throws Exception，对于布控撤控等适用
    public Boolean call() throws Exception{   
        // TODO Auto-generated method stub 
    	
		//消息队列显相关参数
		Connection connection = queueList.getConnection();
		if(connection==null){
			logger.warn("获取消息链接为null");
			return false;
		}
		else{
			Channel channel= connection.createChannel();
    	      
			//生成返回消息队列名称
			String message="";
			String qn="";
			try {
				qn=channel.queueDeclare().getQueue();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				logger.warn("生成临时消息队列失败");
				channel.close();//关闭响应消息通道
				connection.close();//关闭通道连接
				e1.printStackTrace();
				return false;
			}
	
			//生成的消息队列名字
			String MSG0=qn;
			//完整的命令消息
			message=MSG0+CloudFactory.msgsplit+command;
			logger.info("生成的临时消息队列名@@命令消息："+message);
	    	
			//发送命令消息
			try {
				channel.basicPublish("", queueList.Queuelist, null, message.getBytes("UTF-8"));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				logger.warn("向消息服务器发送消息出现异常");
				channel.close();//关闭响应消息通道
				connection.close();//关闭通道连接
				e.printStackTrace();
				return false;
			}   
			
			//等待并处理回复消息(布控撤控操作仅一个消息，故总数1@@第1条消息@@第1个消息结果包)
			long starttime=System.currentTimeMillis();
			long endtime;
			int state=0;//0：表示执行失败，返回结果为false；1：表示执行成功，返回结果为true；4:表示超时；
			boolean autoAck=true;
					
			while(true){
				GetResponse response=channel.basicGet(qn,autoAck);
				if(response==null){
					Thread.sleep(CloudFactory.waitResponseSleep);//如果没有消息，则休眠10秒后再获取消息
				}else{
					String msgstr=new String(response.getBody());
					logger.info("收到MQServer回复的消息 :"+msgstr);
					String[] str0=msgstr.split(CloudFactory.msgsplit);
					if(str0[2].indexOf("TRUE")>=0){
						state=1;
						break;
					}
				}
				//判断是否超时
				endtime=System.currentTimeMillis();
				if(endtime-starttime>CloudFactory.responseTimeout){//对于布控和撤控超过30秒则视为操作失败
					state=4;//超时
					break;
				}
			}
			
			channel.queueDelete(qn);//删除响应消息队列
			channel.close();//关闭响应消息通道
			connection.close();//关闭通道连接
	
			if(state==4||state==0)//超时或者执行失败
			{
				if(state==0)
					logger.warn("命令执行失败");
				if(state==4)
					logger.warn("命令超时");
				return false;
			}
			else{//执行成功	
				logger.info("命令执行成功");
				return true;
			}
		}
			
    }   
}
