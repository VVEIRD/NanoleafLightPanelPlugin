package vv3ird.ESDSoundboardApp.plugins.NanoleafLightPanel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.github.rowak.Aurora;
import io.github.rowak.Setup;
import io.github.rowak.StatusCodeException;
import io.github.rowak.StatusCodeException.UnauthorizedException;
import vv3ird.ESDSoundboardApp.AudioApp;
import vv3ird.ESDSoundboardApp.config.AppConfiguration;
import vv3ird.ESDSoundboardApp.config.Sound;
import vv3ird.ESDSoundboardApp.ngui.pages.Page;
import vv3ird.ESDSoundboardApp.plugins.data.SoundPluginMetadata;
import vv3ird.ESDSoundboardApp.plugins.data.SoundPluginMetadataTemplate;
import vv3ird.ESDSoundboardApp.plugins.data.SoundPluginMetadataTemplate.TYPE;
import vv3ird.ESDSoundboardApp.plugins.listener.PlaybackListener;
import vv3ird.ESDSoundboardApp.plugins.listener.Plugin;
import vv3ird.ESDSoundboardApp.plugins.listener.PluginListener;

public class NanoleafLightPanelPlugin implements Plugin, PlaybackListener {
	
	private static Logger logger = LogManager.getLogger(NanoleafLightPanelPlugin.class);

	List<PluginListener> listener = new LinkedList<>();
	
	private Aurora aurora = null;
	
	String apilevel = "v1";

	public void init() throws UnauthorizedException, StatusCodeException {
		logger.debug("Initializing " + getDisplayName());
		if (isEnabled() && isConfigured()) {
			logger.debug(getDisplayName() + " is enabled and configured connection to aurora");
			try {
				List<InetSocketAddress> auroras = getAuroras();
				for (InetSocketAddress inetSocketAddress : auroras) {
					logger.debug("Aurora in network: " + inetSocketAddress.toString());
					String mac = getMac(inetSocketAddress.getHostName());
					if(AudioApp.getConfig("nanoleaf.accessToken." + mac) != null) {
						logger.debug("Aurora is configured in the app, connecting...");
						aurora = new Aurora(inetSocketAddress, apilevel, AudioApp.getConfig("nanoleaf.accessToken." + mac));
						logger.debug("Aurora \"" + aurora.getName() + "\"is cconnected");
					}
				}
			} catch (IOException e) {
				logger.error("Error connection to aurora");
				logger.error(e);
			}
		}
	}

	public List<InetSocketAddress> getAuroras() throws SocketTimeoutException, UnknownHostException, IOException {
		List<InetSocketAddress> auroras = Setup.findAuroras(5_000);
		return auroras;
	}

	@Override
	public String getDisplayName() {
		// TODO Auto-generated method stub
		return "Nanoleaf Plugin";
	}

	@Override
	public Page getConfigurationPage() {
		return null;
	}

	@Override
	public boolean isEnabled() {
		return "true".equalsIgnoreCase(AudioApp.getConfig("nanoleaf.enabled"));
	}

	@Override
	public void enable() {
		AudioApp.addConfig("nanoleaf.enabled", "true");
	}

	@Override
	public void disable() {
		AudioApp.addConfig("nanoleaf.enabled", "false");
	}

	@Override
	public boolean isConfigured() {
		String[] accessToken = AudioApp.getConfigKeys("nanoleaf.accessToken");
		logger.debug("Access token:");
		for (String string : accessToken) {
			logger.debug(string);
		}
		return accessToken != null && accessToken.length > 0;
	}

	public boolean isConnected() {
		try {
			if (aurora != null) {
				aurora.state().getBrightness();
				return true;
			}
		} catch (StatusCodeException e) {
			logger.error("Error connection to aurora " + aurora.getName() + "(" + aurora.getHostName() + ":" + aurora.getPort() + ")");
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean isPlaybackListener() {
		return true;
	}

	@Override
	public void addPluginListener(PluginListener pl) {
		listener.add(pl);
	}

	@Override
	public void removePluginListener(PluginListener pl) {
		listener.remove(pl);
	}

	@Override
	public boolean hasSoundPluginMetadataTemplates() {
		return true;
	}

	@Override
	public List<SoundPluginMetadataTemplate> getSoundPluginMetadataTemplates() {
		List<String> list = getListForMetadata("Effect");
		SoundPluginMetadataTemplate effect = new SoundPluginMetadataTemplate(NanoleafLightPanelPlugin.class.getCanonicalName(),
				"NanoleafLightPanelPlugin", TYPE.LIST, "Effect", list);

		SoundPluginMetadataTemplate brightness = new SoundPluginMetadataTemplate(NanoleafLightPanelPlugin.class.getCanonicalName(),
				"NanoleafLightPanelPlugin", TYPE.INT, "Brightness", null);

		SoundPluginMetadataTemplate switchOnOff = new SoundPluginMetadataTemplate(NanoleafLightPanelPlugin.class.getCanonicalName(),
				"NanoleafLightPanelPlugin", TYPE.INT, "Switch On/Off", null);
		List<SoundPluginMetadataTemplate> templates = new LinkedList<>();
		templates.add(effect);
		templates.add(brightness);
		templates.add(switchOnOff);
		return templates;
	}

	@Override
	public List<String> getListForMetadata(String metadataName) {
		if (isEnabled() && isConfigured() && this.aurora != null) {
			if ("Effect".equalsIgnoreCase(metadataName)) {
				try {
					return Arrays.asList(aurora.effects().getEffectsList());
				} catch (StatusCodeException e) {
					e.printStackTrace();

				}
			}
		}
		return null;
	}

	//

	@Override
	public void onStart(Sound s) {
		logger.debug(s);
		if (isEnabled() && isConfigured() && isConnected()) {
			List<SoundPluginMetadata> metadata = s.getMetadataFor(NanoleafLightPanelPlugin.class.getCanonicalName());
			if(metadata != null && metadata.size() > 0) {
				SoundPluginMetadata effect     = metadata.stream().filter(m -> "Effect".equalsIgnoreCase(m.key)).findFirst().orElse(null);
				SoundPluginMetadata brightness = metadata.stream().filter(m -> "Brightness".equalsIgnoreCase(m.key)).findFirst().orElse(null);
				SoundPluginMetadata switchOnOf = metadata.stream().filter(m -> "Switch On/Off".equalsIgnoreCase(m.key)).findFirst().orElse(null);
				if(switchOnOf != null) {
					try {
						aurora.state().setOn(switchOnOf.valueInt != 0);
					} catch (StatusCodeException e) {
						logger.error("Error switching aurora on/off: " + switchOnOf.valueInt);
						logger.error(e);
//						e.printStackTrace();
					}
				}if(effect != null) {
					try {
						aurora.effects().setEffect(effect.valueString);
					} catch (StatusCodeException e) {
						logger.error("Error setting new effect: " + effect.valueString);
						logger.error(e);
//						e.printStackTrace();
					}
				}
				if(brightness != null) {
					try {
						aurora.state().fadeToBrightness(brightness.valueInt, 5);
					} catch (StatusCodeException e) {
						logger.error("Error changing brightness: " + brightness.valueInt);
						logger.error(e);
//						e.printStackTrace();
					}
				}
			}
		}
	}

	@Override
	public void onStop(Sound s) {
		// TODO Auto-generated method stub

	}

	private static String getMac(String ip) {
		Pattern macpt = null;

		// Find OS and set command according to OS
		String OS = System.getProperty("os.name").toLowerCase();

		String[] cmd;
		if (OS.contains("win")) {
			// Windows
			macpt = Pattern.compile("[0-9a-f]+-[0-9a-f]+-[0-9a-f]+-[0-9a-f]+-[0-9a-f]+-[0-9a-f]+");
			String[] a = { "arp", "-a", ip };
			cmd = a;
		} else {
			// Mac OS X, Linux
			macpt = Pattern.compile("[0-9a-f]+:[0-9a-f]+:[0-9a-f]+:[0-9a-f]+:[0-9a-f]+:[0-9a-f]+");
			String[] a = { "arp", ip };
			cmd = a;
		}

		try {
			// Run command
			Process p = Runtime.getRuntime().exec(cmd);
			p.waitFor();
			// read output with BufferedReader
			BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line = reader.readLine();

			// Loop trough lines
			while (line != null) {
				Matcher m = macpt.matcher(line);

				// when Matcher finds a Line then return it as result
				if (m.find()) {
					logger.debug("Found");
					logger.debug("MAC: " + m.group(0));
					return m.group(0).replace("-", ":");
				}

				line = reader.readLine();
			}

		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		// Return empty string if no MAC is found
		return "";
	}

}
