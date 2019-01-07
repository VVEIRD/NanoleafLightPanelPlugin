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
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

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
import vveird.TabletopSoundboard.plugins.NanoleafLightPanel.aurora.AuroraServiceDescriptor;

import javax.swing.JPasswordField;

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
	private JComboBox<AuroraServiceDescriptor> cbNanoleafDevices;

	private Map<AuroraServiceDescriptor, AuroraServiceDescriptor> auroras = null;
	private Map<String, String> accessToken = null;
	private JLabel lblMacHasAccessToken;
	private JButton btnRemoveAccesToken;
	
	private boolean isEnabled = false;
	private JButton btnConnect;
	private JButton btnDeleteAccessToken;
	private JPasswordField pwdAccessToken;
	
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

		btnConnect = new JButton("Connect");
		btnConnect.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				connect((AuroraServiceDescriptor)cbNanoleafDevices.getSelectedItem());
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
		lblConnectSuccessful.setBounds(320, 174, 130, 14);
		pnNanoleaf.add(lblConnectSuccessful);

		lblNotSucc = new JLabel("Connect not successful");
		lblNotSucc.setForeground(Color.RED.darker());
		lblNotSucc.setVisible(false);
		lblNotSucc.setBounds(310, 174, 140, 14);
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
				if(accessToken.containsKey(((AuroraServiceDescriptor)cbNanoleafDevices.getSelectedItem()).usn)) {
					lblMacHasAccessToken.setText("Accesstoken already available");
					lblNotSucc.setVisible(false);
					lblConnectSuccessful.setVisible(false);
					updateButtonsForAccessToken(true);
					pwdAccessToken.setText(accessToken.get(((AuroraServiceDescriptor)cbNanoleafDevices.getSelectedItem()).usn));
				}
				else {
					updateButtonsForAccessToken(false);
					lblMacHasAccessToken.setText("");
					pwdAccessToken.setText("");
				}
			}
		});
		// cbSpotifyDevices.setBackground(ColorScheme.SIDE_BAR_BACKGROUND_COLOR);
		// cbSpotifyDevices.setForeground(ColorScheme.SIDE_BAR_FOREGROUND_COLOR);
		cbNanoleafDevices.setBounds(103, 11, 347, 22);
		pnNanoleaf.add(cbNanoleafDevices);
		
		btnRemoveAccesToken = new JButton("Destroy Access Token");
		btnRemoveAccesToken.setToolTipText("This will destroy the access token in the Nanoleaf Light");
		btnRemoveAccesToken.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(accessToken.containsKey(((AuroraServiceDescriptor)cbNanoleafDevices.getSelectedItem()).usn)) {
					try {
						String usn = ((AuroraServiceDescriptor)cbNanoleafDevices.getSelectedItem()).usn;
						removeAccessToken(((AuroraServiceDescriptor) cbNanoleafDevices.getSelectedItem()),
								accessToken.get(usn), usn);
						lblMacHasAccessToken.setText("Access Token destroyed");
						lblNotSucc.setVisible(false);
						lblConnectSuccessful.setVisible(false);
						updateButtonsForAccessToken(false);
					} catch (StatusCodeException e1) {
						lblMacHasAccessToken.setText("Could not destry access token: " + e1.getMessage());
						lblNotSucc.setVisible(false);
						lblConnectSuccessful.setVisible(false);
						e1.printStackTrace();
					}
				}
				else {
					lblMacHasAccessToken.setText("");
					updateButtonsForAccessToken(false);
				}
			}
		});
		btnRemoveAccesToken.setBounds(161, 140, 139, 23);
		add(btnRemoveAccesToken);
		
		lblMacHasAccessToken = new JLabel("");
		lblMacHasAccessToken.setOpaque(false);
		lblMacHasAccessToken.setBounds(10, 205, 440, 14);
		add(lblMacHasAccessToken);
		
		btnDeleteAccessToken = new JButton("Delete Access Token");
		btnDeleteAccessToken.setToolTipText("This only deletes the access token from the app");
		btnDeleteAccessToken.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String usn = ((AuroraServiceDescriptor)cbNanoleafDevices.getSelectedItem()).usn;
				if(accessToken.containsKey(usn)) {
					deleteAccessToken(usn);
					lblMacHasAccessToken.setText("Access Token deleted");
					lblNotSucc.setVisible(false);
					lblConnectSuccessful.setVisible(false);
					updateButtonsForAccessToken(false);
				}
				else {
					lblMacHasAccessToken.setText("");
					updateButtonsForAccessToken(false);
				}
			}
		});
		btnDeleteAccessToken.setBounds(310, 140, 139, 23);
		add(btnDeleteAccessToken);
		
		pwdAccessToken = new JPasswordField();
		pwdAccessToken.getDocument().addDocumentListener(new DocumentListener() {
			
			@Override
			public void removeUpdate(DocumentEvent e) {
				String usn = ((AuroraServiceDescriptor)cbNanoleafDevices.getSelectedItem()).usn;
				if(pwdAccessToken.getPassword().length > 0)
					accessToken.put(usn, new String(pwdAccessToken.getPassword()));
				else
					accessToken.remove(usn);
				updateButtonsForAccessToken(accessToken.containsKey(usn));
				lblMacHasAccessToken.setText("Access token changed");
				lblNotSucc.setVisible(false);
				lblConnectSuccessful.setVisible(false);
			}
			
			@Override
			public void insertUpdate(DocumentEvent e) {
				String usn = ((AuroraServiceDescriptor)cbNanoleafDevices.getSelectedItem()).usn;
				if(pwdAccessToken.getPassword().length > 0)
					accessToken.put(usn, new String(pwdAccessToken.getPassword()));
				else
					accessToken.remove(usn);
				updateButtonsForAccessToken(accessToken.containsKey(usn));
				lblMacHasAccessToken.setText("Access token changed");
				lblNotSucc.setVisible(false);
				lblConnectSuccessful.setVisible(false);
			}
			
			@Override
			public void changedUpdate(DocumentEvent e) {
				String usn = ((AuroraServiceDescriptor)cbNanoleafDevices.getSelectedItem()).usn;
				if(pwdAccessToken.getPassword().length > 0)
					accessToken.put(usn, new String(pwdAccessToken.getPassword()));
				else
					accessToken.remove((String)cbNanoleafDevices.getSelectedItem());
				updateButtonsForAccessToken(accessToken.containsKey(usn));
				lblMacHasAccessToken.setText("Access token changed");
				lblNotSucc.setVisible(false);
				lblConnectSuccessful.setVisible(false);
			}
		});
		pwdAccessToken.putClientProperty("JPasswordField.cutCopyAllowed",true);
		pwdAccessToken.setToolTipText("Access Token");
		pwdAccessToken.setBounds(10, 174, 291, 20);
		add(pwdAccessToken);
		lblConnectSuccessful.setOpaque(false);
		lblNotSucc.setOpaque(false);
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
		List<String> USNs = Arrays.asList(AudioApp.getConfigKeys(NanoleafLightPanelPlugin.NANOLEAF_ACCESS_TOKEN)).stream()
				.map(s -> s.replace(NanoleafLightPanelPlugin.NANOLEAF_ACCESS_TOKEN + ".", "")).collect(Collectors.toList());
		accessToken = new HashMap<>();
		for (String usn : USNs) {
			String at = AudioApp.getConfig(NanoleafLightPanelPlugin.NANOLEAF_ACCESS_TOKEN + "." + usn);
			if (at != null)
				accessToken.put(usn, at);
		}
		// Get all Auroras accessable through the network
		auroras = getAuroras();
		cbNanoleafDevices.setModel(new JComboBox<AuroraServiceDescriptor>(auroras.keySet().toArray(new AuroraServiceDescriptor[0])).getModel());
		String usn = cbNanoleafDevices.getSelectedItem() != null ? ((AuroraServiceDescriptor)cbNanoleafDevices.getSelectedItem()).usn : null;
		if(accessToken.containsKey(usn)) {
			lblMacHasAccessToken.setText("Accesstoken already available");
			lblNotSucc.setVisible(false);
			lblConnectSuccessful.setVisible(false);
			updateButtonsForAccessToken(true);
			pwdAccessToken.setText(accessToken.get(usn));
		}
		else {
			updateButtonsForAccessToken(false);
			lblMacHasAccessToken.setText("");
			pwdAccessToken.setText("");
		}
	}

	private Map<AuroraServiceDescriptor, AuroraServiceDescriptor> getAuroras() {
		List<AuroraServiceDescriptor> availableAuroras = NanoleafLightPanelPlugin.getAvailableAuroras();
		Map<AuroraServiceDescriptor, AuroraServiceDescriptor> auroras = new HashMap<>();
		for (AuroraServiceDescriptor serviceDescriptor : availableAuroras) {
			auroras.put(serviceDescriptor, serviceDescriptor);
		}
		return auroras;
	}

	private void connect(AuroraServiceDescriptor adress) {
		AuroraServiceDescriptor serviceDescriptor = adress;
		if (!accessToken.containsKey(adress.usn))
			try {
				String at = Setup.createAccessToken(serviceDescriptor.address.getHostName(), serviceDescriptor.address.getPort(),
						NanoleafLightPanelPlugin.API_LEVEL);
				accessToken.put(adress.usn, at);
				new Aurora(serviceDescriptor.address, NanoleafLightPanelPlugin.API_LEVEL, at);
				lblConnectSuccessful.setVisible(true);
				lblNotSucc.setVisible(false);
				lblMacHasAccessToken.setText("");
			} catch (StatusCodeException e) {
				lblConnectSuccessful.setVisible(false);
				lblNotSucc.setVisible(true);
				lblMacHasAccessToken.setText("");
				logger.error("Error getting access token");
				logger.error(e);
				e.printStackTrace();
			}
	}

	private void removeAccessToken(AuroraServiceDescriptor serviceDescriptor, String accessToken, String usn) throws UnauthorizedException, InternalServerErrorException, StatusCodeException {
		Setup.destroyAccessToken(serviceDescriptor.address.getHostName(), serviceDescriptor.address.getPort(), NanoleafLightPanelPlugin.API_LEVEL, accessToken);
		deleteAccessToken(usn);
	}

	private void deleteAccessToken(String usn)  {
		AudioApp.removeConfig(NanoleafLightPanelPlugin.NANOLEAF_ACCESS_TOKEN + "." + usn);
		pwdAccessToken.setText("");
		accessToken.remove(usn);
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
			AudioApp.addConfig(NanoleafLightPanelPlugin.NANOLEAF_ACCESS_TOKEN + "." + mac, accToken);
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

	private void updateButtonsForAccessToken(boolean hasAccessToken) {
		btnRemoveAccesToken.setEnabled(hasAccessToken);
		btnDeleteAccessToken.setEnabled(hasAccessToken);
		btnConnect.setEnabled(!hasAccessToken);
	}
}
