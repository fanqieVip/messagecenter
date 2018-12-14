# MessageCenter for Android 安卓消息中心组件
## 支持消息多订阅、不依赖上下文、集成了socket通讯（收发socket消息与普通消息无区别）、使用者可以实现socket拦截器实现数据自由拆装和重连机制
## 更新日志
### 【1.0.10】 2018-12-14 
#### 1.修复自动重连可能引发并发的问题
### 【1.0.5】 2018-12-11 
#### 1.修改SocketInterceptor的receiveServerMsg方法，不再要求返回值
### 【1.0.4】 2018-12-10 
#### 1.新增Socket自动重连，重连机制基于屏幕广播、网络广播及定时器，经过多个项目验证，已稳定可靠!
#### 2.通过MessageCenter.setSocketCheckIntervalTime(10 * 1000)设置定时器循环时间
#### 3.简化老版本中的SocketInterceptor拦截器，除receiveServerMsg方法以外都为可选实现
### 使用方式
```Java
         //初始化消息中心
        //socket连接模块集成的是netty框架
        MessageCenter.create(context)
                .setSocketCheckIntervalTime(10 * 1000)      //设置socket自检定时器时间，默认为10秒
                .host("192.168.0.1").port(8088)             //设置socket连接参数，不需要使用socket也可以不设置
                .socketInterceptor(new MySocketInterceptor());//绑定socket拦截器，不需要使用socket也可以不设置

        //连接socket，如果已经连上socket，会自动先断开再连接；不需要使用socket也可以不调用
        MessageCenter.connectSocket();
        //断开socket连接；不需要使用socket也可以不调用
        MessageCenter.disConnectSocket();

        //发送应用内消息
        Msg insideMsg = Msg.ini("insideTag", new Object());//"insideTag"是订阅消息的标识，new Object()是携带的数据包，可以是任意类型
        insideMsg.destination(Msg.APP_INSIDE);//APP_INSIDE为默认值，可以不用设置，如需发送到服务器则使用APP_OUTSIDE
        MessageCenter.sendMessage(insideMsg);

        //发送到应用外的消息
        Msg outSideMsg = Msg.ini("outsideTag", new Object());
        outSideMsg.destination(Msg.APP_OUTSIDE);
        MessageCenter.sendMessage(outSideMsg);

        //Listener方式创建订阅者
        MessageHandler insideMessageHandler = new MessageHandler("insideTag",
                //收到消息订阅
                new MHanderReceiveListener() {
                    @Override
                    public void receivedMsg(Msg message) {}
                },
                //发送消息订阅，可监听发送状态，主要用于处理发送socket消息回执，不是必须实现的方法
                new MHanderSendListener() {
                    @Override
                    public void sendMsgReturn(Msg message, int sendState) {}
                });
        //Listener方式注册订阅者
        MessageCenter.registMessageHandler(insideMessageHandler);
        //Listener方式注销订阅者。如不再使用一定要记得注销
        MessageCenter.unRegistMessageHandler(insideMessageHandler);

        //注解方式注册订阅者
        MessageCenter.registMessageHandler(this);
        //注解方式注销订阅者。如不再使用一定要记得注销
        MessageCenter.unRegistMessageHandler(this,"outsideTag");
```

### 支持注解
```Java
 /**
     * 注解方式绑定订阅者
     * 发送socket消息监听
     * @param obj 发送的数据包 , 不一定是Object，视你发送的消息类型而定，如果发送的数据包为null，这儿可以不要这个参数
     * @param sendState 发送状态
     */
   @MHanderSendTag("outsideTag")
    void sendOutside(Object obj, int sendState){
        //MHanderSendListener.ACTIVE 发送中
        //MHanderSendListener.SUCCESS 发送成功
        //MHanderSendListener.FAIL 发送失败
    }

    /**
     * 注解方式绑定订阅者
     * 接受socket消息监听 收到的数据包 , 不一定是Object，视你收到的消息类型而定，如果收到的数据包为null，这儿可以不要这个参数
     * @param obj
     */
    @MHanderReceiveTag("outsideTag")
    void receiveOutside(Object obj){

    }

```
### socket自定义拦截器
```Java
/**
     * socket拦截器
     * 可以在这里做数据拆装、重连机制
     */
    public class MySocketInterceptor implements SocketInterceptor {

        /**
         * 这里会收到服务器发来的且已处理成String字符串，你可以根据自己的需求进行包装
         * ReceiveMsg有tag和obj可供你的订阅者使用
         * @param msg 收到的数据包
         * @return
         */
        @Override
        public List<ReceiveMsg> receiveServerMsg(String msg) {
            return null;
        }

        /**
         * 这里用于包装心跳包，你可以根据自己的需求包装成需要的字符串样式
         * @return
         */
        @Override
        public String heartbeat() {
            return null;
        }
        /**
         * socket的连接状态监听
         * @param connetState  CONNECTING：连接中 INTERRUPT：已断开 SUCCESS：连接成功 CANCEL：已取消（主动调用MessageCenter.disConnectSocket()会调用）
         * @param exception 如已断开会返回错误信息
         */
        @Override
        public void connectState(ConnetState connetState, Exception exception) {

        }
    }
```
### 混淆方式
```Xml
-keepclassmembers class **{
    @com.fanjun.messagecenter.annotion.MHanderReceiveTag <methods>;
    @com.fanjun.messagecenter.annotion.MHanderSendTag <methods>;
}
-keepattributes Signature
-keepattributes *Annotation*
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.stream.** { *; }
-keep class com.google.gson.examples.android.model.** { *; }
-keep class com.google.gson.* { *;}
-dontwarn com.google.gson.**
-keepclassmembers class * {
   public <init> (org.json.JSONObject);
}
-keepattributes Signature,InnerClasses
-keepclasseswithmembers class io.netty.** {*;}
-keepnames class io.netty.** {*;}
```
### 依赖
#### Maven
```Xml
<dependency>
  <groupId>com.fanjun</groupId>
  <artifactId>messagecenter</artifactId>
  <version>1.0.10</version>
  <type>pom</type>
</dependency>
```
#### Gradle
```Xml
 implementation 'com.fanjun:messagecenter:1.0.10'
```
