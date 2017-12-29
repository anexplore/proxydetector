#### HTTP/HTTPS Proxy Detector

###### 原理
对（ip, port）发送HTTP(HTTPS)指定请求，判断HTTP Response是否符合预期，符合预期即初判为代理。

###### 步骤
* 发送HTTP请求
* 如果返回正确，则判定为支持HTTP代理
* 继续发送CONNECT
* 如果返回正确，发送HTTPS请求
* 如果HTTPS请求返回正确，则判定为支持HTTPS代理

###### 匿名度检测
通过代理发送请求到自己的proxyjudge server上，通过http request headers判断代理你明度即可

###### 注意
* 扫描出的代理在不同网断可达性不同
* 稳性代理少，一般代理可用时间短，需要不断扫描且需对已扫描出代理周期性验证可用性
* 扫描出代理会存在误判，需要尝试使用不同的HTTP或者HTTPS请求清除假代理
