# ProxyDetector
HTTP PROXY 检测

JUST FOR FUN

TO DO LIST:
- [ ] 添加日志
- [X] 代理活性检测
- [X] 代理等级检测(透明、高匿等)
- [X] 代理速度检测(不具有代表性)
- [X] 代理地域抽取 progess(抽取方法已完成 没有集成到可执行程序)
- [ ] 代理能否跨过GFW
- [ ] 代理IP段分析\代理可用时段分析
- [ ] HTTPS代理检测


# SYN_SENT RETRY ON LINUX:

On Linux however, the default retry cycle ends after just 20 seconds. Linux does send SYN retries somewhat faster than BSD-derived kernels - Linux supposedly sends 5 SYNs in this 20 seconds, but this includes the original packet (the retries are after 3s, 6s, 12s, 24s).

> cat /proc/sys/net/ipv4/tcp_syn_retries

> 5 

> echo 3 > /proc/sys/net/ipv4/tcp_syn_retries


# ProxyJudge.us
主要返回结果:
```
HTTP_HOST = proxyjudge.us
HTTP_CONNECTION = keep-alive
HTTP_UPGRADE_INSECURE_REQUESTS = 1
HTTP_USER_AGENT = Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/55.0.2883.75 Safari/537.36
HTTP_ACCEPT = text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8
HTTP_ACCEPT_ENCODING = gzip, deflate, sdch
HTTP_ACCEPT_LANGUAGE = zh-CN,zh;q=0.8,en;q=0.6,zh-TW;q=0.4
REMOTE_ADDR = 106.39.222.34
REMOTE_PORT = 35769
REQUEST_METHOD = GET
REQUEST_URI = /
REQUEST_TIME_FLOAT = 1481192357.634
REQUEST_TIME = 1481192357
```