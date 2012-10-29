package com.gnychis.awmon.ScanResultParsers;

import java.util.ArrayList;

import com.gnychis.awmon.NetDevDefinitions.Device;

abstract public class ScanResultParser {

	abstract public <T extends Object> ArrayList<Device> returnDevices(ArrayList<T> scanResult);
	
}
