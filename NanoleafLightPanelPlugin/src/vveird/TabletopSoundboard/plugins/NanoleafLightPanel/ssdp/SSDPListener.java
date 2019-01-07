package vveird.TabletopSoundboard.plugins.NanoleafLightPanel.ssdp;

import com.vmichalak.protocol.ssdp.SSDPMessage;

public interface SSDPListener {

	public void notify(SSDPMessage msg);

	public void msearchResponse(SSDPMessage msg);

	public void msearch(SSDPMessage msg);

}
