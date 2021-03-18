package com.fd.proxyscan.utils;


public class IPUtils {

	/**
	 * next valid ip bytes array, startIp array will not change
	 * @param startIp start ip bytes
	 * @return next ip bytes
	 */
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

	/**
	 * change startIp to next Ip, startIp array will change
	 *
	 * @param startIp ip bytes
	 */
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

	/**
	 * @param bytes convert ip bytes to ip dot string
	 * @return ip dot string xxx.xxx.xxx.xxx
	 */
	public static String ipByteToString(byte[] bytes) {
		return String.format("%d.%d.%d.%d", bytes[0] & 0xFF, bytes[1] & 0xFF, bytes[2] & 0xFF, bytes[3] & 0xFF);
	}

}
