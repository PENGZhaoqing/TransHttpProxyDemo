#include <stdlib.h>
#include <stdio.h>
#include <string.h>

#include <pcap/pcap.h>

#include <net/ethernet.h>
#include <netinet/in.h>
#include <netinet/ip.h>
#include <arpa/inet.h>
#include <sys/types.h>
#include <sys/socket.h>

#define FILTER "icmp or tcp"
pcap_t *handle;
void got_packet(u_char *args, const struct pcap_pkthdr*, const u_char *pkt);

int main(int argc, char *argv[])
{
	char *dev ;                        /* The device to sniff on */
	char errbuf[PCAP_ERRBUF_SIZE];     /* Error string */
	struct bpf_program fp;             /* The compiled filter */
	bpf_u_int32 mask;                  /* Our netmask */
	bpf_u_int32 net;                   /* Our IP */

	if (argc != 2) {
		printf("usage: ptprox <dev>\n");
		return EXIT_FAILURE;
	} else {
		dev = argv[1];
	}

	/* Find the properties for the device */
	if (pcap_lookupnet(dev, &net, &mask, errbuf) == -1) {
		printf("warning: %s: could not get network: %s\n", dev, errbuf);
		net  = 0;
		mask = 0;
	}

	/* Open the session in promiscuous mode */
	handle = pcap_open_live(dev, BUFSIZ, 1, 1000, errbuf);
	if (handle == NULL) {
		printf("error: %s: could not open: %s\n", dev, errbuf);
		return EXIT_FAILURE;
	}

	if (pcap_compile(handle, &fp, FILTER, 0, mask) == -1) {
		printf("error: could not compile filter '%s': %s\n", FILTER, pcap_geterr(handle));
		return EXIT_FAILURE;
	}

	if (pcap_setfilter(handle, &fp) == -1) {
		printf("error: could not set filter '%s': %s\n", FILTER, pcap_geterr(handle));
		return EXIT_FAILURE;
	}

	/* Grab a packet */
	int r = pcap_loop(handle, -1, got_packet, NULL);
	printf("pcal_loop() quit with: %d\n", r);

	pcap_close(handle);
	return EXIT_SUCCESS;
}

void got_packet(
	u_char                     *args,
	const struct pcap_pkthdr   *header,
	const u_char               *packet)
{
	const struct ether_header  *ethernet;
	const struct ip            *ip;
	char src_ip_str[16];
	char dst_ip_str[16];

	ethernet = (struct ether_header*) packet;
	if (ethernet->ether_type != ntohs(ETHERTYPE_IP)) {
		printf("ignoring non-ip packet (0x%02X) of length %d\n",
			ntohs(ethernet->ether_type), header->len);
		fflush(stdout);
		return;
	}

	ip = (struct ip*) (ethernet+1);
	strcpy(src_ip_str, inet_ntoa(ip->ip_src));
	strcpy(dst_ip_str, inet_ntoa(ip->ip_dst));

	if (ip->ip_p == IPPROTO_ICMP)
		printf("%15s --> %15s  [ICMP]\n", src_ip_str, dst_ip_str);
	else if (ip->ip_p == IPPROTO_TCP)
		printf("%15s --> %15s  [TCP]\n", src_ip_str, dst_ip_str);
	else
		printf("%15s --> %15s  [%d]\n", src_ip_str, dst_ip_str, ip->ip_p);
	fflush(stdout);

	if (pcap_inject(handle, packet, header->len) == -1) {
		printf("error: unable to proxy packet: %s\n", pcap_geterr(handle));
		fflush(stdout);
	}
}

