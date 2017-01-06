#!/usr/bin/env python2.7

from __future__   import print_function
from argparse     import ArgumentParser
from subprocess   import Popen, STDOUT, PIPE
from socket       import socket, AF_INET, SOCK_STREAM
from time         import sleep
from sys          import stdout
from threading    import Thread;
from mininet.net  import Mininet
from mininet.topo import Topo
from mininet.node import RemoteController
from mininet.node import OVSKernelSwitch

MAGIC_MAC = "00:11:00:11:00:11"
MAGIC_IP  = "10.111.111.111"

class MyTopo(Topo):

	def __init__(self):
		"""Create custom topo."""

		Topo.__init__(self)

		switch1 = self.addSwitch('s1')
		switch2 = self.addSwitch('s2')
		switch3 = self.addSwitch('s3')

		h1 = self.addHost('h1')
		h2 = self.addHost('h2')
		h3 = self.addHost('h3')
		prox = self.addHost('prox')
		
		link1 = self.addLink(h1, switch1)
		link2 = self.addLink(h2, switch1)
		link4 = self.addLink(h3, switch3)
		link0 = self.addLink(prox, switch2)		

		link2 = self.addLink(switch1, switch2)
		link3 = self.addLink(switch2, switch3)
		

class Prox(Thread):

	def __init__(self, node, log=None):

		Thread.__init__(self)
		self.node = node
		self.log  = log

	def run(self):
		if self.log != None:
			self.log = open(self.log, 'w')
		self.proc = self.node.popen(
			["./proxy", "prox-eth0"],
			stdout=self.log, stderr=self.log
		)
		print("proxy is running")
		self.proc.wait()
		
def wait_on_controller():

	s = socket(AF_INET, SOCK_STREAM)
	addr = ("localhost", 6653)

	try:
		s.connect(addr)
		s.close()
		return
	except:
		pass

	print("Waiting on controller", end=""); stdout.flush()

	while True:
		sleep(0.1)
		try:
			s.connect(addr)
			s.close()
			print("")
			return
		except:
			print(".", end=""); stdout.flush()
			continue

def build_prox(psrc):
	gcc_proc = Popen(stdout=PIPE, stderr=STDOUT,
			args=("gcc", psrc, "-o", "proxy", "-l", "pcap")
	)

	r = gcc_proc.wait()
	if r != 0:
		out, _ = gcc_proc.communicate()
		print(out)
		exit(1)

if __name__ == "__main__":

	build_prox("proxy.c")
	wait_on_controller()

	mn = Mininet(
		topo=MyTopo(),
		autoSetMacs=True,
		autoStaticArp=True,
		controller=RemoteController('c0',port=6653),
		switch=OVSKernelSwitch
	)

	mn.start()

	sleep(0.5)
	for src in mn.hosts:
		# get rid of ARP
		src.setARP(ip=MAGIC_IP, mac=MAGIC_MAC)
		src.cmd("ping", "-c1", "-W1", MAGIC_IP)

	px = Prox(mn.getNodeByName("prox"), "ptprox.log")
	px.start()
	mn.interact()


