package com.vmichalak.protocol.ssdp;

import java.net.DatagramPacket;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represent a Device discovered by SSDP.
 * 
 * Source: https://github.com/vmichalak/ssdp-client
 * 
 * MIT License
 * 
 * Copyright (c) 2016 Valentin Michalak
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
public class Device {
    private final String ssdpType;
    private final String ip;
    private final String descriptionUrl;
    private final String server;
    private final String serviceType;
    private final String usn;
    private final Map<String, String> headers;

    public Device(String ssdpType, String ip, String descriptionUrl, String server, String serviceType, String usn, Map<String, String> headers) {
    	this.ssdpType = ssdpType;
        this.ip = ip;
        this.descriptionUrl = descriptionUrl;
        this.server = server;
        this.serviceType = serviceType;
        this.usn = usn;
        this.headers = headers;
    }

    /**
     * Instantiate a new Device Object from a SSDP discovery response packet.
     * @param ssdpResult SSDP Discovery Response packet.
     * @return Device
     */
    public static Device parse(DatagramPacket ssdpResult) {
        HashMap<String, String> headers = new HashMap<String, String>();
    	String ssdpType = null;
        Pattern pattern = Pattern.compile("(.*): (.*)");
        Pattern httpResponsePattern = Pattern.compile("^(HTTP|http)/(?<httpversion>(1|2)\\.\\d) (?<httpresponsecode>\\d{3}) (?<httpresponsedescriptor>.*)");
        Pattern ssdpPattern = Pattern.compile("(?<ssdptype>.*) \\* HTTP/(?<httpversion>.*)");

        String[] lines = new String(ssdpResult.getData()).replace("\r", "").split("\n");
        System.out.println(new String(ssdpResult.getData()));
        for (String line : lines) {
        	Matcher ssdpHeader = ssdpPattern.matcher(line);
        	Matcher httpResponseHeader = httpResponsePattern.matcher(line);
        	if(ssdpHeader.matches()) {
        		ssdpType = ssdpHeader.group("ssdptype");
        	}
        	else if(httpResponseHeader.matches()) {
        		ssdpType = "SSDP-RESPONSE";
        		headers.put("HTTP-RESPONSE-STATUS-CODE", httpResponseHeader.group("httpresponsecode"));
        		headers.put("HTTP-RESPONSE-STATUS-DESC", httpResponseHeader.group("httpresponsedescriptor"));
        	}
            Matcher matcher = pattern.matcher(line);
            if(matcher.matches()) {
                headers.put(matcher.group(1).toUpperCase(), matcher.group(2));
            }
        }
        String location = headers.get("LOCATION");
        String server = headers.get("SERVER");
        String st = headers.get("ST");
        String usn = headers.get("USN");
        headers.remove("LOCATION");
        headers.remove("SERVER");
        headers.remove("ST");
        headers.remove("USN");
        return new Device(
        		ssdpType,
                ssdpResult.getAddress().getHostAddress(),
                location,
                server,
                st,
                usn,
                headers);
    }
    
    public String getSSDPType() {
		return ssdpType;
	}

    public String getIPAddress() {
        return ip;
    }

    public String getLocation() {
        return descriptionUrl;
    }

    public String getServer() {
        return server;
    }

    public String getServiceType() {
        return serviceType;
    }

    public String getUSN() {
        return usn;
    }
    
    public String getHeader(String header) {
    	return this.headers.get(header);
    }
    
    public List<String> getHeaders()  {
    	return new LinkedList<>(this.headers.keySet());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Device device = (Device) o;

        if (ip != null ? !ip.equals(device.ip) : device.ip != null) return false;
        if (descriptionUrl != null ? !descriptionUrl.equals(device.descriptionUrl) : device.descriptionUrl != null)
            return false;
        if (server != null ? !server.equals(device.server) : device.server != null) return false;
        if (serviceType != null ? !serviceType.equals(device.serviceType) : device.serviceType != null) return false;
        return usn != null ? usn.equals(device.usn) : device.usn == null;

    }

    @Override
    public int hashCode() {
        int result = ip != null ? ip.hashCode() : 0;
        result = 31 * result + (descriptionUrl != null ? descriptionUrl.hashCode() : 0);
        result = 31 * result + (server != null ? server.hashCode() : 0);
        result = 31 * result + (serviceType != null ? serviceType.hashCode() : 0);
        result = 31 * result + (usn != null ? usn.hashCode() : 0);
        return result;
    }

    public String toJson() {
        return "Device{" +
                "ip='" + ip + '\'' +
                ", descriptionUrl='" + descriptionUrl + '\'' +
                ", server='" + server + '\'' +
                ", serviceType='" + serviceType + '\'' +
                ", usn='" + usn + '\'' +
                '}';
    }
    
    @Override
    public String toString() {
    	StringBuilder sb = new StringBuilder();
    	sb.append("SSDP Pasckage: \r\n");
    	sb.append("  SSDP Type:  " + this.getSSDPType() + "\r\n");
    	sb.append("  IP:         " + this.getIPAddress() + "\r\n");
    	sb.append("  Location:   " + this.getLocation() + "\r\n");
    	sb.append("  Server:     " + this.getServer() + "\r\n");
    	sb.append("  Servicetype " + this.getServiceType() + "\r\n");
    	sb.append("  USN         " + this.getUSN() + "\r\n");
    	sb.append("  Additional Headers: " + "\r\n");
    	List<String> headers = this.getHeaders();
    	Collections.sort(headers);
		for (String key : headers) {
			sb.append("    " + key + ": " + this.getHeader(key) + "\r\n");
		}
    	// TODO Auto-generated method stub
    	return sb.toString();
    }
}