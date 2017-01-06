package net.floodlightcontroller.transHttpProxy;

import java.util.Collection;
import java.util.Map;
import java.util.List;
import java.util.HashMap;

import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.action.OFActions;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFlowAdd;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPacketIn;

import net.floodlightcontroller.core.types.NodePortTuple;

import net.floodlightcontroller.routing.IRoutingService;
import net.floodlightcontroller.routing.Path;
import net.floodlightcontroller.devicemanager.SwitchPort;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPacket;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.core.IFloodlightProviderService;

import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.OFBufferId;

import net.floodlightcontroller.core.internal.IOFSwitchService;

import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransHttpProxyDemo implements IFloodlightModule, IOFMessageListener {

	protected static final MacAddress MAGIC = MacAddress.of("00:11:00:11:00:11");
	protected static final MacAddress H1 = MacAddress.of("00:00:00:00:00:01");
	protected static final MacAddress H2 = MacAddress.of("00:00:00:00:00:02");
	protected static final MacAddress H3 = MacAddress.of("00:00:00:00:00:03");
	protected static final MacAddress PX = MacAddress.of("00:00:00:00:00:04");

	protected enum RouteMode {
		ROUTE_DIRECT, ROUTE_PROXY, ROUTE_DROP,
	};

	protected Logger log;
	protected IRoutingService routingEngine;
	protected IOFSwitchService switchEngine;
	protected IFloodlightProviderService floodlightProvider;
	protected Map<MacAddress, SwitchPort> mac_to_switchport;

	////////////////////////////////////////////////////////////////////////////
	//
	// IFloodlightModule
	//

	@Override // IFloodlightModule
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IFloodlightProviderService.class);
		return l;
	}

	@Override // IFloodlightModule
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		return null;
	}

	@Override // IFloodlightModule
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		return null;
	}

	@Override // IFloodlightModule
	public void init(FloodlightModuleContext context) throws FloodlightModuleException {
		floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
		routingEngine = context.getServiceImpl(IRoutingService.class);
		switchEngine = context.getServiceImpl(IOFSwitchService.class);
		log = LoggerFactory.getLogger("AdvNetRouter");
		mac_to_switchport = new HashMap<MacAddress, SwitchPort>();
	}

	@Override // IFloodlightModule
	public void startUp(FloodlightModuleContext context) {
		floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
	}

	////////////////////////////////////////////////////////////////////////////
	//
	// IOFMessageListener
	@Override // IFloodlightModule
	public String getName() {
		return "TransHttpProxyDemo";
	}

	@Override
	public boolean isCallbackOrderingPrereq(org.projectfloodlight.openflow.protocol.OFType type, String name) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isCallbackOrderingPostreq(org.projectfloodlight.openflow.protocol.OFType type, String name) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public net.floodlightcontroller.core.IListener.Command receive(IOFSwitch sw, OFMessage msg,
			FloodlightContext cntx) {
		// TODO Auto-generated method stub

		if (msg.getType() != OFType.PACKET_IN) {
			return Command.CONTINUE;
		}

		OFPacketIn pki = (OFPacketIn) msg;
		Ethernet eth = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);

		IPacket p = eth.getPayload();
		if (!(p instanceof IPv4)) {
			return Command.CONTINUE;
		}

		OFPort in_port = (pki.getVersion().compareTo(OFVersion.OF_12) < 0) ? pki.getInPort()
				: pki.getMatch().get(MatchField.IN_PORT);
		OFBufferId bufid = pki.getBufferId();
		MacAddress dl_src = eth.getSourceMACAddress();
		MacAddress dl_dst = eth.getDestinationMACAddress();

		if (dl_dst.equals(MAGIC)) {
			SwitchPort tmp = new SwitchPort(sw.getId(), in_port);
			mac_to_switchport.put(dl_src, tmp);
			send_drop_rule(tmp, bufid, dl_src, dl_dst);
			return Command.STOP;
		}

		process_pkt(sw, in_port, bufid, dl_src, dl_dst);
		return Command.STOP;
	}

	private void process_pkt(IOFSwitch sw, OFPort in_port, OFBufferId bufid, MacAddress dl_src, MacAddress dl_dst) {
		RouteMode rm;
		SwitchPort sp_src, sp_dst, sp_prx;

		log.debug("packet_in: " + sw.getId() + ":" + in_port + " " + dl_src + " --> " + dl_dst);

		sp_src = mac_to_switchport.get(dl_src);
		sp_dst = mac_to_switchport.get(dl_dst);
		sp_prx = mac_to_switchport.get(PX);

		if (sp_src == null) {
			log.error("unknown source port");
			return;
		} else if (sp_dst == null) {
			log.error("unknown dest port");
			return;
		} else if (sp_prx == null) {
			log.error("unknown proxy port");
			return;
		}

		rm = getCommMode(dl_src, dl_dst);
		log.info("packet_in: routing mode: " + rm);

		if (rm == RouteMode.ROUTE_DROP) {

			send_drop_rule(sp_src, bufid, dl_src, dl_dst);

		} else if (rm == RouteMode.ROUTE_PROXY) {

			create_route(sp_src, sp_prx, dl_src, dl_dst, OFBufferId.NO_BUFFER);
			create_route(sp_prx, sp_dst, dl_src, dl_dst, OFBufferId.NO_BUFFER);
			create_route(sp_dst, sp_prx, dl_dst, dl_src, OFBufferId.NO_BUFFER);
			create_route(sp_prx, sp_src, dl_dst, dl_src, bufid);

		} else { // ROUTE_DIRECT

			create_route(sp_src, sp_dst, dl_src, dl_dst, OFBufferId.NO_BUFFER);
			create_route(sp_dst, sp_src, dl_dst, dl_src, bufid);
		}
	}

	private RouteMode getCommMode(MacAddress src, MacAddress dst) {

		// H1 <--> H2 : Direct
		if ((src.equals(H1) && dst.equals(H2)) || (src.equals(H2) && dst.equals(H1))) {
			log.info("pair: H1 <--> H2 : Direct");
			return RouteMode.ROUTE_DIRECT;
		}

		// H1 <--> PX : Drop
		else if ((src.equals(H1) && dst.equals(PX)) || (src.equals(PX) && dst.equals(H1))) {
			log.info("pair: H1 <--> PX : Drop");
			return RouteMode.ROUTE_DROP;
		}

		// H1 <--> H3 : Proxy
		else if ((src.equals(H1) && dst.equals(H3)) || (src.equals(H3) && dst.equals(H1))) {
			log.info("pair: H1 <--> H3 : Proxy");
			return RouteMode.ROUTE_PROXY;
		}

		// H2 <--> PX : Drop
		else if ((src.equals(H2) && dst.equals(PX)) || (src.equals(PX) && dst.equals(H2))) {
			log.info("pair: H2 <--> PX : Drop");
			return RouteMode.ROUTE_DROP;
		}

		// H2 <--> H3 : Proxy
		else if ((src.equals(H2) && dst.equals(H3)) || (src.equals(H3) && dst.equals(H2))) {
			log.info("pair: H2 <--> H3 : Proxy");
			return RouteMode.ROUTE_PROXY;
		}

		// H3 <--> PX : Drop
		else if ((src.equals(H3) && dst.equals(PX)) || (src.equals(PX) && dst.equals(H3))) {
			log.info("pair: H3 <--> PX : Drop");
			return RouteMode.ROUTE_DROP;
		} else {
			return RouteMode.ROUTE_DROP;
		}
	}

	private void create_route(SwitchPort sp_src, SwitchPort sp_dst, MacAddress dl_src, MacAddress dl_dst,
			OFBufferId bufid) {
		Path route = routingEngine.getPath(sp_src.getNodeId(), sp_src.getPortId(), sp_dst.getNodeId(),
				sp_dst.getPortId());

		log.info("Route: " + route);
		List<NodePortTuple> switchPortList = route.getPath();

		for (int indx = switchPortList.size() - 1; indx > 0; indx -= 2) {

			DatapathId dpid = switchPortList.get(indx).getNodeId();
			OFPort out_port = switchPortList.get(indx).getPortId();
			OFPort in_port = switchPortList.get(indx - 1).getPortId();

			write_flow(dpid, in_port, dl_src, dl_dst, out_port, (indx == 1) ? bufid : OFBufferId.NO_BUFFER);
		}
	}

	private void send_drop_rule(SwitchPort sw1, OFBufferId bufid, MacAddress src, MacAddress dst) {
		write_flow(sw1.getNodeId(), sw1.getPortId(), src, dst, null, bufid);
	}

	private void write_flow(DatapathId dpid, OFPort in_port, MacAddress dl_src, MacAddress dl_dst, OFPort out_port,
			OFBufferId bufid) {

		IOFSwitch sw = switchEngine.getSwitch(dpid);
		OFFactory myFactory = sw.getOFFactory();

		List<OFAction> actionList = new ArrayList<OFAction>();
		OFActions actions = myFactory.actions();

		if (out_port != null) {
			OFActionOutput output = actions.buildOutput().setPort(out_port).setMaxLen(0xFFffFFff).build();
			actionList.add(output);
		} else {
			log.info("droping.....");
		}

		Match match = myFactory.buildMatch().setExact(MatchField.ETH_SRC, dl_src).setExact(MatchField.ETH_DST, dl_dst)
				.setExact(MatchField.IN_PORT, in_port).setExact(MatchField.ETH_TYPE, EthType.IPv4).build();

		OFFlowAdd flowAdd = myFactory.buildFlowAdd().setBufferId(bufid).setMatch(match).setIdleTimeout(20)
				.setPriority(1).setActions(actionList).build();

		log.info("writing flowmod: " + flowAdd);

		sw.write(flowAdd);
	}

}
