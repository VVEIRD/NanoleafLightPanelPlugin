package vveird.TabletopSoundboard.plugins.NanoleafLightPanel.aurora;

import java.net.InetSocketAddress;

public class AuroraServiceDescriptor {

	public final InetSocketAddress address;

	public final String locationUrl;

	public final String usn;

	public final String deviceId;

	public final String deviceName;
	
	public AuroraServiceDescriptor(InetSocketAddress address, String locationUrl, String usn, String deviceId,
			String deviceName) {
		super();
		this.address = address;
		this.locationUrl = locationUrl;
		this.usn = usn;
		this.deviceId = deviceId;
		this.deviceName = deviceName;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof AuroraServiceDescriptor))
			return false;
		AuroraServiceDescriptor other = (AuroraServiceDescriptor) obj;
		boolean sameAddress = this.address != null & this.address.equals(other.address);
		boolean sameUSN = this.usn != null && this.usn.equals(other.usn);
		boolean sameDeviceId = this.deviceId != null && this.deviceId.equals(other.deviceId);
		return sameAddress && sameUSN && sameDeviceId;
	}

	@Override
	public String toString() {
		return deviceName;
	}

}
