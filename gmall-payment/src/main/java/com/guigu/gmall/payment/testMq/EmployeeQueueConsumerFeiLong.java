package com.guigu.gmall.payment.testMq;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;

public class EmployeeQueueConsumerFeiLong {
/*消息中间件的处理者    -- 消费端*/

    public static void main(String[] args) {
        // 消息的消费端，性质跟监听器一样，不停的监听ActiveMQ的提供者，抢着拍马屁
        // 老板说：我渴了(提供端)， 那么一群员工(消费端)就抢着给老板倒水，一个员工手疾眼快就抢到杯子，给老板倒好水，那么其他员工就不会再去倒水，这件事就完了，
        //  全体员工监视着老板的一举一动，一旦有什么吩咐就抢着去做，一个人做了，其他人就不会再做

        // 创建连接池
        ConnectionFactory connect = new ActiveMQConnectionFactory(ActiveMQConnection.DEFAULT_USER,ActiveMQConnection.DEFAULT_PASSWORD,"tcp://192.168.75.99:61616");  //
        try {
            Connection connection = connect.createConnection();  // 创建连接
            connection.start(); // 开始连接
            //第一个值表示是否使用事务，如果选择true，第二个值相当于选择0
            //  事务开启，签收必须写Session.SESSION_TRANSACTED  收到消息后，消息并没有真正的被消费。消息只是被锁住。一旦出现该线程死掉、抛异常，或者程序执行了session.rollback()那么消息会释放，重新回到队列中被别的消费端再次消费。
            //  事务不开启，签收方式选择 Session.AUTO_ACKNOWLEDGE 只要调用comsumer.receive方法 ，自动确认。
            //  事务不开启，签收方式选择 Session.CLIENT_ACKNOWLEDGE 需要客户端执行 message.acknowledge(),否则视为未提交状态，线程结束后，其他线程还可以接收到。  这种方式跟事务模式很像，区别是不能手动回滚,而且可以单独确认某个消息。
            //  事务不开启，签收方式选择 Session.DUPS_OK_ACKNOWLEDGE 在Topic模式下做批量签收时用的，可以提高性能。但是某些情况消息可能会被重复提交，使用这种模式的consumer要可以处理重复提交的问题。
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            // 拍哪个提供者的马屁    -- 创建消息排列
            Destination testqueue = session.createQueue("HESHUI");

            // 消费端对象
            MessageConsumer consumer = session.createConsumer(testqueue);

            // 监听消息
            consumer.setMessageListener(new MessageListener() {
                @Override
                public void onMessage(Message message) {
                    if(message instanceof TextMessage){
                        try {
                            String text = ((TextMessage) message).getText();
                            // 执行消息
                            System.err.println(text+"飞龙老师听到此话，手疾眼快，拿起杯子去倒水");
                            //session.rollback();
                        } catch (JMSException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                }
            });
            /*时时监听，所以不关闭连接*/

        }catch (Exception e){
            e.printStackTrace();;
        }

    }
}
