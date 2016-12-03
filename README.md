# proxydetector
HTTP PROXY 检测

JUST FOR FUN

TO DO LIST:
- [ ] 代理活性检测
- [ ] 代理等级检测(透明、高匿等)
- [ ] 代理速度检测(不具有代表性)
- [ ] 代理地域抽取
- [ ] 代理能否跨过GFW
- [ ] 代理IP段分析
- [ ] HTTPS代理检测

SYN_SENT RETRY ON LINUX:

On Linux however, the default retry cycle ends after just 20 seconds. Linux does send SYN retries somewhat faster than BSD-derived kernels - Linux supposedly sends 5 SYNs in this 20 seconds, but this includes the original packet (the retries are after 3s, 6s, 12s, 24s).

> cat /proc/sys/net/ipv4/tcp_syn_retries

> 5 

> echo 3 > /proc/sys/net/ipv4/tcp_syn_retries
