package com.guigu.gmall.payment.testMq;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQTextMessage;

import javax.jms.*;
// 队列模式的消息生产者/消息发送者
public class BossQueueProducer {

    public static void main(String[] args) {
       // 创建ActiveMQ工厂
        ConnectionFactory connect = new ActiveMQConnectionFactory("tcp://192.168.75.99:61616"); // ActiveMQ连接地址
        try {
            // 创建连接
            Connection connection = connect.createConnection();
            // 获取连接
            connection.start();
            //第一个值表示是否使用事务，如果选择true，第二个值相当于选择0
            //   事务开启:只执行send并不会提交到队列中，只有当执行session.commit()时，消息才被真正的提交到队列中。
            //   事务不开启:只要执行send，就进入到队列中。
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);//事务会话

            // 创建消息队列
            Queue testqueue = session.createQueue("HESHUI"); // 队列名，  消费者根据队列名，时时监听
            // 消息的发送者(提供者)
            MessageProducer producer = session.createProducer(testqueue);

            // 消息内容
            TextMessage textMessage=new ActiveMQTextMessage();
            textMessage.setText("我是boss，我渴了，谁来给我倒一杯水！"); // 内容
            //持久化与非持久化
            producer.setDeliveryMode(DeliveryMode.PERSISTENT); //设置为持久化，持久化的好处就是当activemq宕机的话，消息队列中的消息不会丢失。非持久化会丢失。但是会消耗一定的性能。
            /*  比如：老板说口渴了，此时办公室一个人都没有，使用持久化，老板说口渴了这句话就会印在杯子上，只要有员工进办公室，就会去消费这条信息，去倒水*/

            // 发出消息
            producer.send(textMessage);
            // 提交事务
            session.commit();
            // 释放连接
            connection.close();

        } catch (JMSException e) {
            e.printStackTrace();
        }


    }
}
