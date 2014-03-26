package com.hualu.cloud.clientinterface.offline;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;

import com.hualu.cloud.basebase.staticparameter;
import com.hualu.cloud.clientinterface.CloudFactory;
import com.hualu.cloud.clientinterface.realtime.queueList;
import com.hualu.cloud.clientinterface.realtime.warnlistener;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.GetResponse;
import com.rabbitmq.client.QueueingConsumer;

/**
 * 用于处理离线业务，与commander的区别主要返回消息格式不同，而且可能返回多条消息
 * @author yanghy
 *
 */
public class commander2 implements Callable{
    String  command = null;   
    public static Logger logger = Logger.getLogger(commander2.class);
    
    public commander2(String command) {   
        super();
        this.command = command;   
    }   

    @Override  
    //public Boolean call() throws Exception，对于布控撤控等适用
    public ArrayList<String> call() throws Exception{   
        // TODO Auto-generated method stub 
    	ArrayList<String> record=new ArrayList();
    	
		//确定消息队列的名字
		Connection connection = queueList.getConnection();
	    Channel channel= connection.createChannel();
    	      
		//生成返回的消息队列名称qn
		String message="";
		String qn="";
		try {
			qn=channel.queueDeclare().getQueue();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		//完整的命令消息：返回消息队列名称+@@+命令
		String MSG0=qn;		
		message=MSG0+CloudFactory.msgsplit+command;
		logger.info("command2 发送消息："+message);
    	

		//发送命令消息
		try {
			channel.basicPublish("", queueList.Queuelist, null, message.getBytes());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}   
		
		//等待并处理回复消息(记录总数M@@返回消息N条@@共p条消息@@消息1@@消息2)
		long starttime=System.currentTimeMillis();
		long endtime;
		int state=0;//0:开始接受消息；1：收到记录；2：没有返回数据；3：接受完毕；4：超时退出;5:执行成功
		boolean autoAck=true;
		int msgnum=0;//返回消息计数
		int recordnum0=0;//返回消息记录数
		int resultnum0=0;//消息总条数
		int msgnum0=1;//返回消息条数
		logger.debug("command2.java run(),recordnum:"+record.size());
		
		//QueueingConsumer consumer = new QueueingConsumer(channel);  
		//channel.basicConsume(qn, true,consumer);  
		while(true){
			if(msgnum>=msgnum0){
				logger.debug("command2.java run(),msgnum:"+msgnum);
				state=3;//记录读取完毕
				break;
			}
			GetResponse response=channel.basicGet(qn,autoAck);
			if(response==null){
				Thread.sleep(CloudFactory.waitResponseSleep);
			}else{
				String msgstr=new String(response.getBody());
				//System.out.println("command2.java run(),收到返回消息:"+msgstr);
				if(state==0){//取出第一条记录，获得该命令最先一条记录
					//第一条记录格式：记录总数M@@返回消息N条@@共p条消息@@消息1@@消息2
					state=1;
					msgnum=1;//收到一条消息
					String[] str0=msgstr.split(CloudFactory.msgsplit);
					//System.out.println("消息长度："+str0.length);
					resultnum0=Integer.parseInt(str0[0]);
					recordnum0=Integer.parseInt(str0[1]);
					msgnum0=Integer.parseInt(str0[2]);
					if(resultnum0==0||recordnum0==0){//如果总记录数为0，或者返回记录数为0，表示无返回结果，state置为2
						state=2;
						break;
					}
					record.add(msgstr);
					//System.out.println("if command2.java run(),recordnum:"+record.size());
				}
				else{//如果状态非0，则进行具体记录消息解析的状态
					msgnum+=1;//收到消息累计值加1
					msgstr=new String(response.getBody());
					record.add(msgstr);
					//System.out.println("else command2.java run(),recordnum:"+record.size());
				}
			
				//判断是否超时
				endtime=System.currentTimeMillis();
				if(endtime-starttime>CloudFactory.responseTimeout){
					state=4;//超时
					break;
				}
			}
		}

		logger.debug("commander2.java run():关闭response消息通道，state="+state+" 含义：1：收到记录；2：没有返回数据；3：接受完毕；4：超时退出;5:执行成功");
		channel.queueDelete(qn);//删除消息队列
		channel.close();
		connection.close();//关闭通道
		switch(state){
		case 1:
			logger.info("命令执行成功，返回记录数记录"+record.size());
		case 2:
			logger.info("命令执行成功，但返回记录数记录为0");
		case 4:
			logger.warn("命令超时");
		case 0:
			logger.warn("命令执行失败");
		default:
			logger.warn("命令执行的结果状态非常态：state="+state);
		}

		return record;

    }   
}
