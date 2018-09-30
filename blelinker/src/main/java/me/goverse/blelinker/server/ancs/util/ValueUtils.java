package me.goverse.blelinker.server.ancs.util;

import me.goverse.blelinker.server.ancs.constant.AncsID;
import me.goverse.blelinker.server.ancs.constant.AncsOffset;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author gaoyu
 */
public class ValueUtils {

	/**
	 * obtainNotificationSource
	 * @param eventID eventID
	 * @param eventFlags eventFlags
	 * @param categoryID categoryID
	 * @param categoryCount categoryCount
	 * @param notificationUID notificationUID
	 * @return byte[]
	 */
    public static byte[] obtainNotificationSource(byte eventID, byte eventFlags,
    		byte categoryID, byte categoryCount, byte[] notificationUID){
    	byte[] value = new byte[8];
    	value[AncsOffset.NS.EVENTID] = eventID;
    	value[AncsOffset.NS.EVENTFLAGS] = eventFlags;
    	value[AncsOffset.NS.CATEGORYID] = categoryID;
    	value[AncsOffset.NS.CATEGORYCOUNT] = categoryCount;
    	System.arraycopy(notificationUID, 0, value, AncsOffset.NS.NOTIFICATIONUID, 4);
    	return value;
    }


	/**
	 * convertIntToBBLE
	 * @param myInteger myInteger
	 * @return byte[]
	 */
	public static byte[] convertIntToBBLE(int myInteger){
	    return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(myInteger).array();
	}

	/**
	 * convertBBToIntLE
	 * @param byteBarray byteBarray
	 * @return int
	 */
	public static int convertBBToIntLE(byte [] byteBarray){
	    return ByteBuffer.wrap(byteBarray).order(ByteOrder.LITTLE_ENDIAN).getInt();
	}


	/**
	 * convertIntToBBBE
	 * @param myInteger myInteger
	 * @return byte[]
	 */
	public static byte[] convertIntToBBBE(int myInteger){
	    return ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(myInteger).array();
	}


	/**
	 * convertBBToIntBE
	 * @param byteBarray byteBarray
	 * @return int
	 */
	public static int convertBBToIntBE(byte [] byteBarray){
	    return ByteBuffer.wrap(byteBarray).order(ByteOrder.BIG_ENDIAN).getInt();
	}

	/**
	 * spiltToMTUs
	 * @param value value
	 * @param mtuSize mtuSize
	 * @return MTUs
	 */
	public static List<byte[]> spiltToMTUs(byte[] value, int mtuSize) {

		if (null == value) {
			return null;
		}

		List<byte[]> mtus = new ArrayList<>();
		if (mtuSize == Integer.MAX_VALUE) {

			mtus.add(value);
		} else if (mtuSize > 0) {

			int mtu = mtuSize;
			int start;
			int end;
			int remaining = value.length % mtu;

			if (remaining != 0) {
				end = value.length - mtu;
			} else {
				end = value.length;
			}

			for (start = 0; (start + mtu) <= end; start += mtu) {
				mtus.add(Arrays.copyOfRange(value, start, start + mtu));
			}

			if (remaining != 0) {
				if (remaining == 1) {
					mtu = (value.length - start) / 2 + 1;
				}

				for (; (start + mtu) <= value.length; start += mtu) {
					mtus.add(Arrays.copyOfRange(value, start, start + mtu));
				}

				if (start != value.length) {
					mtus.add(Arrays.copyOfRange(value, start, value.length));
				}
			}
		}
		return mtus;
	}

	/**
	 * putString
	 * @param bb ByteBuffer
	 * @param string string
	 * @param maxLen maxLen
	 * @return String
	 */
	public static String putString(ByteBuffer bb, String string, int maxLen) {

		byte[] message = string.getBytes(AncsID.CHARSET);
		String subStr = null;
		maxLen = message.length > maxLen ? maxLen : message.length;
		if (bb.remaining() <= 2) {
			return "";
		} else if (bb.remaining() <= maxLen + 2) {
			maxLen = bb.remaining() - 2;
		}
		subStr = truncateString(string, maxLen, AncsID.CHARSET);
		message = subStr.getBytes();
		bb.put((byte) (0xff & message.length));
		bb.put((byte) (0xff & (message.length >> 8)));
		bb.put(message);
		return subStr;
	}

	/**
	 * truncateString
	 * @param input input
	 * @param n n
	 * @param charset charset
	 * @return String
	 */
	public static String truncateString(String input, int n, Charset charset) {
		int start = 0;
		int end = input.length();
		int mid;
		byte[] bMid;
		do{
			mid = (start + end + 1) /2;
			bMid = input.substring(0, mid).getBytes(charset);
			if(mid == start || mid == end){
				if(bMid.length > n){
					return input.substring(0, mid-1);
				}else{
					return input.substring(0, mid);
				}
			}
			if(bMid.length == n){
				return input.substring(0, mid);
			}else if(bMid.length > n){
				end = mid;
			}else if(bMid.length < n){
				start = mid;
			}
		}while(true);
	}
}
