package com.fd.proxyscan.utils;

public class IPUtils {
	
	public static byte[] nextIp(byte[] startIp) {
		byte[] nextIp = new byte[startIp.length];
		System.arraycopy(startIp, 0, nextIp, 0, startIp.length);
		for (int i = 3; i >= 0; i--) {
			if ((nextIp[i] & 0xFF) < 254) {
				nextIp[i]++;
				break;
			} else {
				nextIp[i] = 1;
			}
		}
		int top = (nextIp[0] & 0xFF);
		if (top == 172 || top == 192) {
			nextIp[0]++;
		}
		return nextIp;
	}
	
	public static void changeToNextIp(byte[] startIp) {
		for (int i = 3; i >= 0; i--) {
			if ((startIp[i] & 0xFF) < 254) {
				startIp[i]++;
				break;
			} else {
				startIp[i] = 1;
			}
		}
		int top = (startIp[0] & 0xFF);
		if (top == 172 || top == 192) {
			startIp[0]++;
		}
	}
}
