## 分布式Websocket

当服务器以集群模式部署时，每个服务器都提供了一个websocket服务，由于存在负载均衡中间件，那么用户的websocket会连接到其中一个服务器；

假设现在有服务器A和服务器B，用户a连接了A，用户b连接了B，a想给b发送消息，但是b不能收到消息，因为服务器A和B的session并不共享，也就是连接不同服务器的用户不能相互通信。




### 解决方案

这里使用一种比较直观的方式来实现分布式websocket，需要引入消息中间件，同时所有的websocket服务器都能连接到同一个消息中间件；

在这个演示中，使用了rabbitmq作为消息队列

1. 用户上线后，将记录当前用户id和channel的映射
2. 消息队列上有一个扇形交换机，每个服务器上线后声明一个队列和这个扇形交换机绑定，并消费队列上的消息
3. 当服务器收到websocket消息后，直接将消息包装后投递到消息队列的扇形交换机，同时这个消息也会广播到每一个绑定这个交换机的队列
4. 当服务从消息队列收到消息后，对消息拆包，主要关注消息中destination属性，即用户id, 这个属性说明了这个消息将要发送到哪里去；服务器检查当前上下文中是否存在这个用户id对应的session，如果存在，将消息发送给用户，否则不处理
5. 用这种方式处理点对点和广播通信

在这个模式中，点对点和广播都使用了同一种方式处理。在处理点对点时，消息会被传播到所有的队列，只不过只有存在目标用户的服务器会处理消息。在广播模式下，所有的服务器都会处理消息。

![示意图](https://github.com/applesack/distributed-ws/blob/master/image/diagram.png)



### 方案存在的问题

在点对点操作时，消息仍然会被广播到每一个消息队列，集群中的服务越多，利用率越低。




### 演示

按照配置和部署章节完成服务启动后，在浏览器输入地址`http://localhost:7000`打开网页，按照下图进行操作。

其中服务端默认会提高10个ws服务器，端口从`7001`至`7010`，例如`ws://localhost:7003`

![示意图](https://github.com/applesack/distributed-ws/blob/master/image/demonstraion.png)




### 配置和部署

项目使用maven作为构架工具，jdk版本为17，依赖中间件为rabbitmq，在resource目录下的config-dev中配置rabbitmq的路径和账号信息，账号需要具有管理员权限

#### maven编译运行

```shell
mvn clean
mvn install
mvn package
cd target
java -jar distributed-ws-0.1-fat.jar
```

#### docker

先执行maven打包，然后执行下面的命令构建

```shell
docker build -t fd/ws-app . 
```
或者使用docker-compose
```shell
docker-compose up -d
```



### TODO

- 引入protobuf来处理websocket服务器到消息队列之间的通信
