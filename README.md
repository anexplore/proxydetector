# proxydetector
HTTP PROXY 检测

JUST FOR FUN

ON LINUX:
On Linux however, the default retry cycle ends after just 20 seconds. Linux does send SYN retries somewhat faster than BSD-derived kernels - Linux supposedly sends 5 SYNs in this 20 seconds, but this includes the original packet (the retries are after 3s, 6s, 12s, 24s).

> cat /proc/sys/net/ipv4/tcp_syn_retries

5 

> echo 3 > /proc/sys/net/ipv4/tcp_syn_retries
