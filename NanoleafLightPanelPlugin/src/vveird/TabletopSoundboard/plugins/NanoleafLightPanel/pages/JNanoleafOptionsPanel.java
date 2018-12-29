package vveird.TabletopSoundboard.plugins.NanoleafLightPanel.pages;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.border.EtchedBorder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.github.rowak.Aurora;
import io.github.rowak.Setup;
import io.github.rowak.StatusCodeException;
import io.github.rowak.StatusCodeException.InternalServerErrorException;
import io.github.rowak.StatusCodeException.UnauthorizedException;
import vveird.TabletopSoundboard.AudioApp;
import vveird.TabletopSoundboard.ngui.plugins.JPluginConfigurationPanel;
import vveird.TabletopSoundboard.ngui.util.ColorScheme;
import vveird.TabletopSoundboard.ngui.util.Helper;
import vveird.TabletopSoundboard.plugins.NanoleafLightPanel.NanoleafLightPanelPlugin;

public class JNanoleafOptionsPanel extends JPluginConfigurationPanel {

	private static Logger logger = LogManager.getLogger(JNanoleafOptionsPanel.class);

	/**
	 * 
	 */
	private static final long serialVersionUID = 5693479059309814273L;

	private NanoleafLightPanelPlugin plugin = null;
	
	private JPanel pnNanoleaf;
	private JLabel lblConnectSuccessful;
	private JLabel lblNotSucc;
	private JComboBox<String> cbNanoleafDevices;

	private Map<String, InetSocketAddress> auroras = null;
	private Map<String, String> accessToken = null;
	private JLabel lblMacHasAccessToken;
	private JButton btnRemoveAccesToken;
	
	private boolean isEnabled = false;
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public JNanoleafOptionsPanel(NanoleafLightPanelPlugin plugin) {
		this.plugin = Objects.requireNonNull(plugin);
		this.isEnabled = this.plugin.isEnabled();
		setOpaque(false);
		this.setBackground(ColorScheme.MAIN_BACKGROUND_COLOR);
		this.setOpaque(false);
		this.setLayout(null);

		pnNanoleaf = this;
		pnNanoleaf.setOpaque(false);
		pnNanoleaf.setBorder(new EtchedBorder(EtchedBorder.LOWERED, ColorScheme.MAIN_BACKGROUND_COLOR.brighter(),
				ColorScheme.MAIN_BACKGROUND_COLOR.darker()));
		pnNanoleaf.setSize(650, 230);
		pnNanoleaf.setPreferredSize(new Dimension(650, 230));
		pnNanoleaf.setMinimumSize(new Dimension(650, 230));
		pnNanoleaf.setMaximumSize(new Dimension(650, 230));
		pnNanoleaf.setLayout(null);

		JButton btnConnect = new JButton("Connect");
		btnConnect.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				connect(auroras.get(cbNanoleafDevices.getSelectedItem()));
			}
		});
		btnConnect.setBounds(10, 140, 141, 23);
		pnNanoleaf.add(btnConnect);

		JTextPane txtpnToEnableSpotify = new JTextPane();
		txtpnToEnableSpotify.setEditable(false);
		txtpnToEnableSpotify.setText(
				"1. Select the Nanoleaf you want to connect to.\r\n2. Press the Power Button of that Nanoleaf Panel for 5-7 seconds to activate its pairing mode.\r\n3. Press the Connect Button below.\r\n4. A success messsage should appear, press \"Save\".");
		txtpnToEnableSpotify.setOpaque(false);
		txtpnToEnableSpotify.setForeground(ColorScheme.FOREGROUND_COLOR);
		txtpnToEnableSpotify.setBounds(10, 44, 440, 85);
		pnNanoleaf.add(txtpnToEnableSpotify);

		lblConnectSuccessful = new JLabel("Connect successful");
		lblConnectSuccessful.setVisible(false);
		lblConnectSuccessful.setForeground(Color.GREEN.darker());
		lblConnectSuccessful.setBounds(171, 144, 130, 14);
		pnNanoleaf.add(lblConnectSuccessful);

		lblNotSucc = new JLabel("Connect not successful");
		lblNotSucc.setForeground(Color.RED.darker());
		lblNotSucc.setVisible(false);
		lblNotSucc.setBounds(161, 144, 140, 14);
		pnNanoleaf.add(lblNotSucc);

		JLabel lblActiveDevice = new JLabel("Device:");
		lblActiveDevice.setForeground(ColorScheme.FOREGROUND_COLOR);
		lblActiveDevice.setFont(Helper.defaultUiFont);
		lblActiveDevice.setBounds(10, 13, 83, 14);
		pnNanoleaf.add(lblActiveDevice);
		String[] wait = new String[] { "Loading Devices... Pleas wait" };
		cbNanoleafDevices = new JComboBox(wait);
		cbNanoleafDevices.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(accessToken.containsKey(cbNanoleafDevices.getSelectedItem())) {
					lblMacHasAccessToken.setText("Accesstoken already available");
					btnRemoveAccesToken.setVisible(true);
				}
				else {
					btnRemoveAccesToken.setVisible(false);
					lblMacHasAccessToken.setText("");
				}
			}
		});
		// cbSpotifyDevices.setBackground(ColorScheme.SIDE_BAR_BACKGROUND_COLOR);
		// cbSpotifyDevices.setForeground(ColorScheme.SIDE_BAR_FOREGROUND_COLOR);
		cbNanoleafDevices.setBounds(103, 11, 347, 22);
		pnNanoleaf.add(cbNanoleafDevices);
		
		btnRemoveAccesToken = new JButton("Remove Accestoken");
		btnRemoveAccesToken.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(accessToken.containsKey(cbNanoleafDevices.getSelectedItem())) {
					try {
						removeAccessToken(auroras.get(cbNanoleafDevices.getSelectedItem()), accessToken.get(cbNanoleafDevices.getSelectedItem()), (String)cbNanoleafDevices.getSelectedItem());
						lblMacHasAccessToken.setText("Access Token destroyed");
					} catch (StatusCodeException e1) {
						lblMacHasAccessToken.setText("Could not destry access token: " + e1.getMessage());
						e1.printStackTrace();
					}
				}
				else
					lblMacHasAccessToken.setText("");
			}
		});
		btnRemoveAccesToken.setBounds(320, 140, 130, 23);
		add(btnRemoveAccesToken);
		
		lblMacHasAccessToken = new JLabel("");
		lblMacHasAccessToken.setBounds(10, 174, 440, 14);
		add(lblMacHasAccessToken);
		Thread t = new Thread(new Runnable() {

			@Override
			public void run() {
				initAuroras();
			}
		});
		t.start();
	}

	private void initAuroras() {
		// Load configured Access Token
		List<String> macs = Arrays.asList(AudioApp.getConfigKeys("nanoleaf.accessToken")).stream()
				.map(s -> s.replace("nanoleaf.accessToken.", "")).collect(Collectors.toList());
		accessToken = new HashMap<>();
		for (String mac : macs) {
			String at = AudioApp.getConfig("nanoleaf.accessToken." + mac);
			if (at != null)
				accessToken.put(mac, at);
		}
		// Get all Auroras accessable through the network
		auroras = getAuroras();
		cbNanoleafDevices.setModel(new JComboBox<String>(auroras.keySet().toArray(new String[0])).getModel());
		if(accessToken.containsKey(cbNanoleafDevices.getSelectedItem())) {
			lblMacHasAccessToken.setText("Accesstoken already available");
			btnRemoveAccesToken.setVisible(true);
		}
		else {
			btnRemoveAccesToken.setVisible(false);
			lblMacHasAccessToken.setText("");
		}
	}

	private Map<String, InetSocketAddress> getAuroras() {
		List<InetSocketAddress> availableAuroras = NanoleafLightPanelPlugin.getAvailableAuroras();
		Map<String, InetSocketAddress> auroras = new HashMap<>();
		for (InetSocketAddress inetSocketAddress : availableAuroras) {
			auroras.put(NanoleafLightPanelPlugin.getMac(inetSocketAddress.getHostName()), inetSocketAddress);
		}
		return auroras;
	}

	private void connect(InetSocketAddress adress) {
		String mac = (String) cbNanoleafDevices.getSelectedItem();
		InetSocketAddress inet = auroras.get(mac);
		if (!accessToken.containsKey(mac))
			try {
				String at = Setup.createAccessToken(inet.getHostName(), inet.getPort(),
						NanoleafLightPanelPlugin.API_LEVEL);
				accessToken.put(mac, at);
				new Aurora(inet, NanoleafLightPanelPlugin.API_LEVEL, at);
				lblConnectSuccessful.setVisible(true);
				lblNotSucc.setVisible(false);
			} catch (StatusCodeException e) {
				lblConnectSuccessful.setVisible(false);
				lblNotSucc.setVisible(true);
				logger.error("Error getting access token");
				logger.error(e);
				e.printStackTrace();
			}
	}

	private void removeAccessToken(InetSocketAddress inet, String at, String mac) throws UnauthorizedException, InternalServerErrorException, StatusCodeException {
		Setup.destroyAccessToken(inet.getHostName(), inet.getPort(), NanoleafLightPanelPlugin.API_LEVEL, at);
		AudioApp.removeConfig("nanoleaf.AccessToken." + mac);
	}

	@Override
	public boolean isConfigured() {
		return accessToken.size()>0;
	}

	@Override
	public void save() {
		AudioApp.addConfig("nanoleaf.enabled", String.valueOf(isEnabled));
		for (String mac : accessToken.keySet()) {
			String accToken = accessToken.get(mac);
			AudioApp.addConfig("nanoleaf.accessToken." + mac, accToken);
		}
	}
	
	@Override
	public void enablePlugin() {
		isEnabled = true;
	}
	
	@Override
	public void disablePlugin() {
		isEnabled = false;
	}
	
	@Override
	public boolean isEnabled() {
		return isEnabled;
	}
	
	@Override
	public boolean initPlugin() {
		if(isConfigured())
			try {
				this.plugin.init();
				return this.plugin.isConnected();
			} catch (StatusCodeException e) {
				logger.error("Cound not initialize the Nanleaf Aurora Plugin " + e.getMessage());
				logger.error(e);
				e.printStackTrace();
			}
		return false;
	}
	
	@Override
	public String getPluginName() {
		// TODO Auto-generated method stub
		return this.plugin.getDisplayName();
	}
}
