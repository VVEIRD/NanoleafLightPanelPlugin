package vveird.TabletopSoundboard.plugins.NanoleafLightPanel.internal;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.connectsdk.discovery.provider.ssdp.SSDPClient;
import com.vmichalak.protocol.ssdp.Device;

import vveird.TabletopSoundboard.AudioApp;

public class AuroraLightsDevices {
	
	private static Logger logger = LogManager.getLogger(AuroraLightsDevices.class);

	private static final String AURORA_SERVICE_TYPE = "nanoleaf_aurora:light";

	public static int TIMEOUT = 5_000;

	private static final List<Thread> ifaceListener = new ArrayList<>(5);
	
	private static List<InetSocketAddress> availableAuroras = new LinkedList<>();

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
					dt.looping = false;
					dt.run();
					dt.looping = true;
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
	
	public static List<InetSocketAddress> getAvailableAuroras() {
		return new ArrayList<>(availableAuroras);
	}
	
	private static class DiscoverThread implements Runnable{
		
		private static Logger logger = LogManager.getLogger(DiscoverThread.class);
		
		private SSDPClient client = null;
		
		private boolean looping = true;
		
		public DiscoverThread(SSDPClient client) {
			this. client = client;
		}
		
		@Override
		public void run() {
			do {
				int sleep = 30_000;
				int timeout = 5_000;
				try {
					client.setTimeout(timeout);
					client.send(SSDPClient.getSSDPSearchMessage(AURORA_SERVICE_TYPE));
					Device d = Device.parse(client.responseReceive());
					Matcher urlMatcher = RegexHelper.HTTP_PATTERN.matcher(d.getLocation());
					if("SSDP-RESPONSE".equalsIgnoreCase(d.getSSDPType()) && AURORA_SERVICE_TYPE.equalsIgnoreCase(d.getServiceType()) && urlMatcher.matches()) {
						String domain = urlMatcher.group("domain");
						String port = urlMatcher.group("port") != null ? urlMatcher.group("port") : "80";
						InetSocketAddress in = new InetSocketAddress(domain, Integer.valueOf(port));
						availableAuroras.add(in);
						
					}
				} catch (IOException e) {
					if (!(e instanceof SocketTimeoutException))
						logger.error("Error encountered while searching for aurora lights", e);
				}
				try {
					sleep = AudioApp.getConfigAsInt("nanoleaf.ssdp.discovery");
					timeout = AudioApp.getConfigAsInt("nanoleaf.ssdp.timeout");
					if(looping)
						Thread.sleep(sleep);
				} catch (NumberFormatException | InterruptedException e) {
					if(e instanceof NumberFormatException)
						sleep = 30_000;
					logger.error("Error encountered on thread sleep", e);
					e.printStackTrace();
				}
			} while(looping);
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
