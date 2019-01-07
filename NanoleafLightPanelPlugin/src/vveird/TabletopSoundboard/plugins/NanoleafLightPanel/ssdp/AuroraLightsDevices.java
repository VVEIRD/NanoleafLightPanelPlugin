package vveird.TabletopSoundboard.plugins.NanoleafLightPanel.ssdp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.connectsdk.discovery.provider.ssdp.SSDPClient;
import com.vmichalak.protocol.ssdp.SSDPMessage;

import vveird.TabletopSoundboard.AudioApp;
import vveird.TabletopSoundboard.plugins.NanoleafLightPanel.aurora.AuroraDiscoveryListener;
import vveird.TabletopSoundboard.plugins.NanoleafLightPanel.aurora.AuroraServiceDescriptor;

public class AuroraLightsDevices {
	
	private static Logger logger = LogManager.getLogger(AuroraLightsDevices.class);

	private static final String AURORA_SERVICE_TYPE = "nanoleaf_aurora:light";

	public static int TIMEOUT = 5_000;

	private static final List<Thread> ifaceListener = new ArrayList<>(5);

	private static List<AuroraDiscoveryListener> discoveryListeners = new ArrayList<>(5);
	
	private static List<AuroraServiceDescriptor> availableAuroras = new LinkedList<>();

	static {
		try {
			List<InetAddress> ipv4Adresses = new ArrayList<>();
				ipv4Adresses = Arrays
						.asList(InetAddress.getAllByName(InetAddress.getLocalHost().getHostName())).stream()
						.filter(i -> RegexHelper.IP_V_4_PATTERN.matcher(i.toString()).matches()).collect(Collectors.toList());
			// Init SSDP Threads
			for (InetAddress ipv4Adress : ipv4Adresses) {
				try {
					SSDPClient ssdpClient = new SSDPClient(ipv4Adress);
					ssdpClient.setTimeout(TIMEOUT);
					DiscoverThread dt = new DiscoverThread(ssdpClient);
					// Initial discovery
					dt.addListener(new DiscoverListener(ipv4Adress.toString()));
					dt.discoverAuroras();
					ssdpClient.setTimeout(0);
					// Async discovery
					Thread t = new Thread(dt);
					t.setName("AuroraDiscovery(" + ipv4Adress + ")");
					t.setDaemon(true);
					t.start();
					ifaceListener.add(t);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} catch (UnknownHostException e) {
			logger.error("unkonwn host", e);
		}
	}
	
	public static List<AuroraServiceDescriptor> getAvailableAuroras() {
		return new ArrayList<>(availableAuroras);
	}

	private static void addAvailableAurora(AuroraServiceDescriptor in) {
		if(!availableAuroras.contains(in)) {
			availableAuroras.add(in);
			for (AuroraDiscoveryListener discoveryListener : discoveryListeners) {
				discoveryListener.auroraJoined(in);
			}
		}
	}

	private static void removeAvailableAurora(AuroraServiceDescriptor in) {
		if (availableAuroras.contains(in)) {
			logger.debug("Aurora Light left the network: " + in.deviceName);
			availableAuroras.remove(in);
			for (AuroraDiscoveryListener discoveryListener : discoveryListeners) {
				discoveryListener.auroraLeft(in);
			}
		}
	}

	public static void addDiscoveryListener(AuroraDiscoveryListener l) {
		discoveryListeners.add(l);
	}

	public static void removeDiscoveryListener(AuroraDiscoveryListener l) {
		discoveryListeners.remove(l);
	}
	
	//
	// Sub-Classes
	//
	private static class DiscoverThread implements Runnable {
		
		private static Logger logger = LogManager.getLogger(DiscoverThread.class);
		
		private List<SSDPListener> listeners = new LinkedList<>();
		
		private SSDPClient client = null;
		
		private boolean looping = true;
		
		public DiscoverThread(SSDPClient client) {
			this. client = client;
		}
		
		public void discoverAuroras() {
			int timeout = 5_000;
			timeout = AudioApp.getConfigAsInt("nanoleaf.ssdp.timeout");
			try {
				client.setTimeout(timeout);
				client.send(SSDPClient.getSSDPSearchMessage(AURORA_SERVICE_TYPE));
				SSDPMessage msg = SSDPMessage.parse(client.responseReceive(), client);
				Matcher urlMatcher = RegexHelper.HTTP_PATTERN.matcher(msg.getLocation());
				if(msg.isMSearchResponse() && AURORA_SERVICE_TYPE.equalsIgnoreCase(msg.getServiceType()) && urlMatcher.matches()) {
					String domain = urlMatcher.group("domain");
					String port = urlMatcher.group("port") != null ? urlMatcher.group("port") : "80";
					InetSocketAddress in = new InetSocketAddress(domain, Integer.valueOf(port));
					fireSSDPEvent(msg);
				}
				client.setTimeout(0);
			} catch (IOException e) {
				if (!(e instanceof SocketTimeoutException))
					logger.error("Error encountered while searching for aurora lights", e);
			}
			
		}
		
		@Override
		public void run() {
			do {
				try {
					logger.debug("Waiting for Notify");
					SSDPMessage d = SSDPMessage.parse(client.multicastReceive(), client);
					fireSSDPEvent(d);
					client.setTimeout(0);
				} catch (IOException e) {
					if (!(e instanceof SocketTimeoutException))
						logger.error("Error encountered while searching for aurora lights", e);
				}
			} while(looping);
		}
		
		public void addListener(SSDPListener listener) {
			this.listeners.add(listener);
		}
		
		private void fireSSDPEvent(SSDPMessage msg) {
			logger.debug("SSDP message recieved: " + msg.getSSDPType() + String.format("(ST: %s, USN: %s, Location: %s, NTS: %s)",
					msg.getServiceType(), msg.getUSN(), msg.getLocation(), msg.getNTS()));
			if(msg.isNotify()) {
				for (SSDPListener ssdpListener : listeners) {
					ssdpListener.notify(msg);
				}
			}
			else if(msg.isMSearch()) {
				for (SSDPListener ssdpListener : listeners) {
					ssdpListener.msearch(msg);
				}
			}
			else if(msg.isMSearchResponse()) {
				for (SSDPListener ssdpListener : listeners) {
					ssdpListener.msearchResponse(msg);
				}
			}
		}
	}
	

	private static class DiscoverListener implements SSDPListener, Runnable {
		
		private static Logger logger = LogManager.getLogger(DiscoverListener.class);
		
		private Map<String, SSDPMessage> cachedSSDPMessages = new HashMap<>();
		
		private Thread expireCacheThread = null;
		
		public DiscoverListener(String expireThreadId) {
			expireCacheThread = new Thread(this);
			expireCacheThread.setDaemon(true);
			expireCacheThread.setName("ExpireSSDPCacheThread(" + expireThreadId + ")");
			expireCacheThread.start();
		}

		@Override
		public void notify(SSDPMessage msg) {
			Matcher urlMatcher = RegexHelper.HTTP_PATTERN.matcher(msg.getLocation());
			if (msg.isAlive() && AURORA_SERVICE_TYPE.equalsIgnoreCase(msg.getServiceType()) && urlMatcher.matches()) {
				cachedSSDPMessages.put(msg.getUSN(), msg);
				String domain = urlMatcher.group("domain");
				String port = urlMatcher.group("port") != null ? urlMatcher.group("port") : "80";
				InetSocketAddress in = new InetSocketAddress(domain, Integer.valueOf(port));
				AuroraServiceDescriptor as = new AuroraServiceDescriptor(in, msg.getLocation(), msg.getUSN(), msg.getHeader("NL-DEVICEID"), msg.getHeader("NL-DEVICENAME"));
				addAvailableAurora(as);
			}
			else if (msg.isByeBye()) {
				msg = cachedSSDPMessages.get(msg.getUSN()) != null ? cachedSSDPMessages.remove(msg.getUSN()) : msg;
				urlMatcher = RegexHelper.HTTP_PATTERN.matcher(msg.getLocation());
				if(urlMatcher.matches()) {
					String domain = urlMatcher.group("domain");
					String port = urlMatcher.group("port") != null ? urlMatcher.group("port") : "80";
					InetSocketAddress in = new InetSocketAddress(domain, Integer.valueOf(port));
					AuroraServiceDescriptor as = new AuroraServiceDescriptor(in, msg.getLocation(), msg.getUSN(), msg.getHeader("NL-DEVICEID"), msg.getHeader("NL-DEVICENAME"));
					removeAvailableAurora(as);
				}
			}
		}
		
		@Override
		public void msearchResponse(SSDPMessage msg) {
			Matcher urlMatcher = RegexHelper.HTTP_PATTERN.matcher(msg.getLocation());
			if (msg.isMSearchResponse() && AURORA_SERVICE_TYPE.equalsIgnoreCase(msg.getServiceType()) && urlMatcher.matches()) {
				cachedSSDPMessages.put(msg.getUSN(), msg);
				String domain = urlMatcher.group("domain");
				String port = urlMatcher.group("port") != null ? urlMatcher.group("port") : "80";
				InetSocketAddress in = new InetSocketAddress(domain, Integer.valueOf(port));
				AuroraServiceDescriptor as = new AuroraServiceDescriptor(in, msg.getLocation(), msg.getUSN(), msg.getHeader("NL-DEVICEID"), msg.getHeader("NL-DEVICENAME"));
				addAvailableAurora(as);
			}
		}

		@Override
		public void msearch(SSDPMessage msg) {
			
		}

		@Override
		public void run() {
			while(true) {
				List<String> removeUSNs = new LinkedList<>();
				List<String> usns = new LinkedList<>(cachedSSDPMessages.keySet());
				for (int i = 0; i < usns.size(); i++) {
					String usn = usns.get(i);
					if(cachedSSDPMessages.get(usn) != null && cachedSSDPMessages.get(usn).isExpired()) {
						removeUSNs.add(usn);
					}
				}
				for (String usn : removeUSNs) {
					notify(cachedSSDPMessages.get(usn).createByeBye());
				}
				try {
					Thread.sleep(5_000);
				} catch (InterruptedException e) {
					logger.error("error on sleep", e);
				}
			}
		}
		
	}

	public static void main(String[] args) throws UnknownHostException {
		Pattern ip4Pattern = Pattern.compile("(?<hostname>.*)/(?<ip>\\d+\\.\\d+\\.\\d+\\.\\d+)(?<add>.*)");
		List<InetAddress> ipv4Adresses = Arrays.asList(InetAddress.getAllByName(InetAddress.getLocalHost().getHostName())).stream().filter(i -> ip4Pattern.matcher(i.toString()).matches()).collect(Collectors.toList());
		System.out.println("InetAdresses for " + InetAddress.getLocalHost().getHostName());
		for (InetAddress inetAddress : ipv4Adresses) {
			System.out.println(inetAddress);
		}
//		for (int i = 0; i < inetadd.length; i++) {
//			System.out.println(inetadd[i] + (ip4Pattern.matcher(inetadd[i].toString()).matches() ? " IPv4" : " IPv6"));
//		}
	}

}
