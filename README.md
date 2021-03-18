### Http/Socks5/socks4a Proxy Detector

#### 1、 原理
对（ip, port）发送HTTP指定请求，判断HTTP Response是否符合预期，符合预期即初判为代理。

#### 2、 步骤
##### HTTP探测
* 发送CONNECT
* 如果返回正确，发送HTTPS请求
* 如果HTTPS请求返回正确，则判定为支持HTTPS代理

##### Socks探测 
* 发送socks connect请求
* 发送HTTP请求
* 响应结果正常，则判定为支持Socks代理

#### 3、 匿名度检测
通过代理发送请求到自己的proxyjudge server上，通过http request headers判断代理匿明度即可

#### 4、 用法
打包: 
~~~shell script
mvn package
~~~

JAR 文件: proxyscan-2.0-jar-with-dependencies.jar

启动命令:
~~~shell script
java -cp proxyscan-2.0-jar-with-dependencies.jar com.fd.proxyscan.NettyProxyScanner 123.0.0.0 80,1080,1081,3128 1000
~~~

参数: 
* 123.0.0.0 起始IP
* 80,1080,1081,3128 需要探测的IP
* 1000 TCP最大链接数

其它配置:
* 调整系统ulimit中nofile等参数来支持大并发链接数
* 注意connectTimeout与net.ipv4.tcp_syn_retries参数的配合
* 流量的估算: 一般单个SYN请求/响应包大小为54(14 + 20 + 20)/66(14 + 20 + 32)/74(14 + 20 + 40)等,因为代理量很少忽略正常链接的流量; 假设能有十分之一的请求能够得到RST类响应且不进行SYN重发, 那么流量可以估算=(并发数 * 74 + 并发数 * 54 / 10) * 8 / 1024 (Kb/s)

#### 5、 注意
* 扫描出的代理在不同网断可达性不同
* 稳性代理少，一般代理可用时间短，需要不断扫描且需对已扫描出代理周期性验证可用性
* 扫描出代理会存在误判，需要尝试使用不同的HTTP或者HTTPS请求清除假代理

