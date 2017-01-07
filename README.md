# TransHttpProxyDemo

Transparent HTTP Proxy App developed by Floodlight


## Files and Directories

* floodlight.log: The eclipse console log while running floodlight project
* mininet.log: Mininet console log runnig in terminal
* proxy.c: Script c code for packet forwarding in prox host  
* proxy: Compiled binary c code of proxy.c
* proxy.log: Printed log file gennerated by proxy running in the prox host
* run.py: Python script to auto compile and run proxy code, and also launch the mininet
* switch-flows.log: the log file of the ovs switch flows table


## Usage

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
  3. Compiling the proxy.c and running the compiled proxy in prox host
  
