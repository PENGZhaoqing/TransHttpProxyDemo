# TransHttpProxyDemo

### [中文教程1](http://blog.csdn.net/ppp8300885/article/details/54136834) 
### [中文教程2](http://blog.csdn.net/ppp8300885/article/details/54193708)

Transparent HTTP Proxy App developed by Floodlight


## Files and Directories

* floodlight.log: The eclipse console log while running floodlight project
* mininet.log: Mininet console log runnig in terminal
* proxy.c: Script c code for packet forwarding in prox host  
* proxy: Compiled binary c code of proxy.c
* proxy.log: Printed log file gennerated by proxy running in the prox host
* run.py: Python script to auto compile and run proxy code, and also launch the mininet
* switch-flows.log: the log file of the ovs switch flows table


## Runing Guide

1. Run the floodlight project in eclipse to start the controller, which is listening for Openflow switches on [0.0.0.0]:6653, you may find the useful tutorial of running floodlight [here](https://floodlight.atlassian.net/wiki/display/floodlightcontroller/Installation+Guide#suk=)

  ```
  2017-01-07 10:58:21.707 INFO  [n.f.c.m.FloodlightModuleLoader] Loading modules from src/main/resources/floodlightdefault.properties
  2017-01-07 10:58:21.883 WARN  [n.f.r.RestApiServer] HTTPS disabled; HTTPS will not be used to connect to the REST API.
  2017-01-07 10:58:21.883 WARN  [n.f.r.RestApiServer] HTTP enabled; Allowing unsecure access to REST API on port 8080.
  2017-01-07 10:58:21.883 WARN  [n.f.r.RestApiServer] CORS access control allow ALL origins: true
  2017-01-07 10:58:22.57 WARN  [n.f.c.i.OFSwitchManager] SSL disabled. Using unsecure connections between Floodlight and switches.
  2017-01-07 10:58:22.57 INFO  [n.f.c.i.OFSwitchManager] Clear switch flow tables on initial handshake as master: TRUE
  2017-01-07 10:58:22.57 INFO  [n.f.c.i.OFSwitchManager] Clear switch flow tables on each transition to master: TRUE
  2017-01-07 10:58:22.63 INFO  [n.f.c.i.OFSwitchManager] Setting 0x1 as the default max tables to receive table-miss flow
  2017-01-07 10:58:22.124 INFO  [n.f.c.i.OFSwitchManager] OpenFlow version OF_15 will be advertised to switches. Supported fallback versions [OF_10, OF_11, OF_12, OF_13, OF_14, OF_15]
  2017-01-07 10:58:22.125 INFO  [n.f.c.i.OFSwitchManager] Listening for OpenFlow switches on [0.0.0.0]:6653
  ...
  ```

2. Run `sudo ./run.py` and it will do following things as ordered for us: 

  1. Trying to connect to the controller with socket (localhost,port=6653), and waiting until success
  2. Launching Mininet and creating the whole virtual network with specified topological structure including Open vswitch and hosts
  3. Compiling the proxy.c and running the compiled proxy in virtual prox host
  
## Experiment and results
 
1. If everything works as intended, you are prompted to the mininet console and you can do following operations to check the network:


  * nodes
  * net
  * h1 ping h3
  * h2 ping h3
  * h1 ping h2
  * h1 ping prox
  
  Follwing example console log shows the results as expeceted: 
  
  ```
  peng@peng-virtual-machine:~/Downloads/TransHttpProxy$ sudo ./run.py
  proxy is running
  mininet> nodes
  available nodes are: 
  c0 h1 h2 h3 prox s1 s2 s3
  mininet> net
  h1 h1-eth0:s1-eth1
  h2 h2-eth0:s1-eth2
  h3 h3-eth0:s3-eth1
  prox prox-eth0:s2-eth1
  s1 lo:  s1-eth1:h1-eth0 s1-eth2:h2-eth0 s1-eth3:s2-eth2
  s2 lo:  s2-eth1:prox-eth0 s2-eth2:s1-eth3 s2-eth3:s3-eth2
  s3 lo:  s3-eth1:h3-eth0 s3-eth2:s2-eth3
  c0
  mininet> h1 ping h3
  PING 10.0.0.3 (10.0.0.3) 56(84) bytes of data.
  64 bytes from 10.0.0.3: icmp_seq=2 ttl=64 time=1634 ms
  64 bytes from 10.0.0.3: icmp_seq=3 ttl=64 time=1629 ms
  64 bytes from 10.0.0.3: icmp_seq=4 ttl=64 time=1629 ms
  64 bytes from 10.0.0.3: icmp_seq=5 ttl=64 time=1629 ms
  ^C
  --- 10.0.0.3 ping statistics ---
  7 packets transmitted, 4 received, 42% packet loss, time 6016ms
  rtt min/avg/max/mdev = 1629.310/1630.597/1634.350/2.166 ms, pipe 2
  mininet> h2 ping h3
  PING 10.0.0.3 (10.0.0.3) 56(84) bytes of data.
  64 bytes from 10.0.0.3: icmp_seq=2 ttl=64 time=1996 ms
  64 bytes from 10.0.0.3: icmp_seq=3 ttl=64 time=1996 ms
  64 bytes from 10.0.0.3: icmp_seq=4 ttl=64 time=1993 ms
  64 bytes from 10.0.0.3: icmp_seq=5 ttl=64 time=1993 ms
  64 bytes from 10.0.0.3: icmp_seq=6 ttl=64 time=1993 ms
  ^C
  --- 10.0.0.3 ping statistics ---
  8 packets transmitted, 5 received, 37% packet loss, time 7014ms
  rtt min/avg/max/mdev = 1993.094/1994.576/1996.525/2.127 ms, pipe 2
  mininet> h1 ping h2
  PING 10.0.0.2 (10.0.0.2) 56(84) bytes of data.
  64 bytes from 10.0.0.2: icmp_seq=2 ttl=64 time=0.195 ms
  64 bytes from 10.0.0.2: icmp_seq=3 ttl=64 time=0.074 ms
  64 bytes from 10.0.0.2: icmp_seq=4 ttl=64 time=0.052 ms
  64 bytes from 10.0.0.2: icmp_seq=5 ttl=64 time=0.081 ms
  ^C
  --- 10.0.0.2 ping statistics ---
  5 packets transmitted, 4 received, 20% packet loss, time 4007ms
  rtt min/avg/max/mdev = 0.052/0.100/0.195/0.056 ms
  mininet> h1 ping prox
  PING 10.0.0.4 (10.0.0.4) 56(84) bytes of data.
  ^C
  --- 10.0.0.4 ping statistics ---
  9 packets transmitted, 0 received, 100% packet loss, time 8057ms

  mininet> exit
  ```
 
2. Cat the proxy.log, we can find the prox host receive the packages from other hosts and then simply sends out all packets it receives on the same port
  
   ```
   10.0.0.1 -->        10.0.0.3  [ICMP]
   10.0.0.3 -->        10.0.0.1  [ICMP]
   10.0.0.1 -->        10.0.0.3  [ICMP]
   10.0.0.3 -->        10.0.0.1  [ICMP]
   10.0.0.1 -->        10.0.0.3  [ICMP]
   10.0.0.3 -->        10.0.0.1  [ICMP]
   10.0.0.1 -->        10.0.0.3  [ICMP]
   10.0.0.3 -->        10.0.0.1  [ICMP]
   10.0.0.1 -->        10.0.0.3  [ICMP]
   10.0.0.3 -->        10.0.0.1  [ICMP]
   ...
   ```
   
3. Run `sudo ovs-ofctl dump-flows s1` int terminal to check the flows table in switch

    ```
    peng@peng-virtual-machine:~$ sudo ovs-ofctl dump-flows s1
    NXST_FLOW reply (xid=0x4):
     cookie=0x0, duration=9.825s, table=0, n_packets=8, n_bytes=784, idle_timeout=20, idle_age=1, priority=1,ip,in_port=1,dl_src=00:00:00:00:00:01,dl_dst=00:00:00:00:00:03 actions=output:3
     cookie=0x0, duration=9.825s, table=0, n_packets=7, n_bytes=686, idle_timeout=20, idle_age=0, priority=1,ip,in_port=3,dl_src=00:00:00:00:00:03,dl_dst=00:00:00:00:00:01 actions=output:1
     cookie=0x0, duration=209.230s, table=0, n_packets=57, n_bytes=6431, idle_age=4, priority=0 actions=CONTROLLER:65535
    peng@peng-virtual-machine:~$ sudo ovs-ofctl dump-flows s2
    NXST_FLOW reply (xid=0x4):
     cookie=0x0, duration=11.232s, table=0, n_packets=8, n_bytes=784, idle_timeout=20, idle_age=3, priority=1,ip,in_port=2,dl_src=00:00:00:00:00:01,dl_dst=00:00:00:00:00:03 actions=output:1
     cookie=0x0, duration=11.228s, table=0, n_packets=8, n_bytes=784, idle_timeout=20, idle_age=2, priority=1,ip,in_port=1,dl_src=00:00:00:00:00:01,dl_dst=00:00:00:00:00:03 actions=output:3
     cookie=0x0, duration=11.228s, table=0, n_packets=8, n_bytes=784, idle_timeout=20, idle_age=2, priority=1,ip,in_port=3,dl_src=00:00:00:00:00:03,dl_dst=00:00:00:00:00:01 actions=output:1
     cookie=0x0, duration=11.228s, table=0, n_packets=8, n_bytes=784, idle_timeout=20, idle_age=1, priority=1,ip,in_port=1,dl_src=00:00:00:00:00:03,dl_dst=00:00:00:00:00:01 actions=output:2
     cookie=0x0, duration=210.640s, table=0, n_packets=87, n_bytes=10534, idle_age=6, priority=0 actions=CONTROLLER:65535
    peng@peng-virtual-machine:~$ sudo ovs-ofctl dump-flows s3
    NXST_FLOW reply (xid=0x4):
     cookie=0x0, duration=12.783s, table=0, n_packets=8, n_bytes=784, idle_timeout=20, idle_age=3, priority=1,ip,in_port=2,dl_src=00:00:00:00:00:01,dl_dst=00:00:00:00:00:03 actions=output:1
     cookie=0x0, duration=12.783s, table=0, n_packets=8, n_bytes=784, idle_timeout=20, idle_age=3, priority=1,ip,in_port=1,dl_src=00:00:00:00:00:03,dl_dst=00:00:00:00:00:01 actions=output:2
     cookie=0x0, duration=212.208s, table=0, n_packets=48, n_bytes=5649, idle_age=7, priority=0 actions=CONTROLLER:65535
     ```
  
  
