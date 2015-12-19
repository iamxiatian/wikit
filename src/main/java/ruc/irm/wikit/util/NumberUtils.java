package ruc.irm.wikit.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Compress & decompress numbers
 * @TODO change class name to NumberCompression
 *
 * @author Tian Xia
 * @date May 03, 2015 1:36 PM
 */
public class NumberUtils {
    /**
     * 浮点转换为字节
     *
     * @param f
     * @return
     */
    public static byte[] float2Bytes(float f) {
        // 把float转换为byte[]
        int fbit = Float.floatToIntBits(f);

        byte[] b = new byte[4];
        for (int i = 0; i < 4; i++) {
            b[i] = (byte) (fbit >> (24 - i * 8));
        }

        // 翻转数组
        int len = b.length;
        // 建立一个与源数组元素类型相同的数组
        byte[] dest = new byte[len];
        // 为了防止修改源数组，将源数组拷贝一份副本
        System.arraycopy(b, 0, dest, 0, len);
        byte temp;
        // 将顺位第i个与倒数第i个交换
        for (int i = 0; i < len / 2; ++i) {
            temp = dest[i];
            dest[i] = dest[len - i - 1];
            dest[len - i - 1] = temp;
        }

        return dest;
    }

    public static float bytes2Float(byte[] b, float defaultValue) {
        if (b == null || b.length < 4) {
            return defaultValue;
        } else {
            return bytes2Float(b);
        }
    }

    /**
     * 字节转换为浮点
     *
     * @param b 字节（至少4个字节）
     * @return
     */
    public static float bytes2Float(byte[] b) {
        int l;
        l = b[0];
        l &= 0xff;
        l |= ((long) b[1] << 8);
        l &= 0xffff;
        l |= ((long) b[2] << 16);
        l &= 0xffffff;
        l |= ((long) b[3] << 24);
        return Float.intBitsToFloat(l);
    }

    /**
     * Convert long to bytes, for small integer, only low-digits are stored in
     * bytes, and all high zero digits are removed to save memory. For example:
     * number 1 should convert to one byte [00000001], number 130
     * should convert to two bytes [00000001, 00000010]
     * @return
     */
    public static byte[] long2Bytes(long num) {
        byte[] buffer = new byte[8];
        for (int ix = 0; ix < 8; ++ix) {
            int offset = 64 - (ix + 1) * 8;
            buffer[ix] = (byte) ((num >> offset) & 0xff);
        }

        int i = 0;
        for (i = 0; i < buffer.length-1; i++) {
            byte b = buffer[i];
            if (b != 0) {
                break;
            }
        }

        //remove zero high bytes
        byte[] buffer2 = new byte[8-i];
        for (int j = 0; j < buffer2.length; j++) {
            buffer2[j] = buffer[i+j];
        }
        return buffer2;
    }

    /**
     * convert bytes to long. high byte store at left.
     * @param bytes
     * @return
     */
    public static long bytes2Long(byte[] bytes) {
        long num = 0;
        for (int ix = 0; ix < bytes.length; ++ix) {
            num <<= 8;
            num |= (bytes[ix] & 0xff);
        }
        return num;
    }

    public static long bytes2Long(byte[] bytes, long defaultValue) {
        if (bytes == null) {
            return defaultValue;
        } else {
            return bytes2Long(bytes);
        }
    }

    /**
     * Convert int to bytes, for small integer, only low-digits are stored in
     * bytes, and all high zero digits are removed to save memory. For example:
     * number 1 should convert to one byte [00000001], number 130
     * should convert to two bytes [00000001, 00000010]
     * @return
     */
    public static byte[] int2Bytes(int num) {
        byte[] buffer = new byte[4];
        for (int ix = 0; ix < 4; ++ix) {
            int offset = 32 - (ix + 1) * 8;
            buffer[ix] = (byte) ((num >> offset) & 0xff);
        }

        int i = 0;
        for (i = 0; i < buffer.length-1; i++) {
            byte b = buffer[i];
            if (b != 0) {
                break;
            }
        }

        //remove zero high bytes
        byte[] buffer2 = new byte[4-i];
        for (int j = 0; j < buffer2.length; j++) {
            buffer2[j] = buffer[i+j];
        }
        return buffer2;
    }

    public static int bytes2Int(byte[] bytes, int defaultValue) {
        if (bytes == null) {
            return defaultValue;
        } else {
            return bytes2Int(bytes);
        }
    }

    /**
     * convert bytes to integer. high byte store at left.
     * @param bytes
     * @return
     */
    public static int bytes2Int(byte[] bytes) {
        int num = 0;
        for (int ix = 0; ix < bytes.length; ++ix) {
            num <<= 8;
            num |= (bytes[ix] & 0xff);
        }
        return num;
    }

    public static int[] bytes2IntArray(byte[] bytes) {
        if (bytes == null || bytes.length<4) {
            return new int[0];
        }
        int[] array = new int[bytes.length/4];
        int byteIndex =0;
        int arrayIndex =0;
        while (byteIndex < bytes.length) {
            int v = bytes[byteIndex++]&0xff;
            v <<=8;
            v |= (bytes[byteIndex++]&0xff);
            v <<=8;
            v |= (bytes[byteIndex++]&0xff);
            v <<=8;
            v |= (bytes[byteIndex++]&0xff);
            array[arrayIndex++] = v;
        }
        return array;
    }

    public static byte[] intArray2Bytes(int[] array) {
        byte[] bytes = new byte[array.length*4];
        int byteIndex=0;
        int arrayIndex=0;
        while (arrayIndex < array.length) {
            int num = array[arrayIndex++];
            bytes[byteIndex++] = (byte)((num>>24) & 0xff);
            bytes[byteIndex++] = (byte)((num>>16) & 0xff);
            bytes[byteIndex++] = (byte)((num>>8) & 0xff);
            bytes[byteIndex++] = (byte)(num & 0xff);
        }
        return bytes;
    }

    public static Set<Integer> bytes2IntSet(byte[] bytes) {
        Set<Integer> numbers = new HashSet<>();
        if (bytes == null || bytes.length < 4) {
            return numbers;
        }
        int byteIndex = 0;
        while (byteIndex < bytes.length) {
            int v = bytes[byteIndex++] & 0xff;
            v <<= 8;
            v |= (bytes[byteIndex++] & 0xff);
            v <<= 8;
            v |= (bytes[byteIndex++] & 0xff);
            v <<= 8;
            v |= (bytes[byteIndex++] & 0xff);
            numbers.add(v);
        }
        return numbers;
    }

    public static byte[] intSet2Bytes(Set<Integer> intSet) {
        byte[] bytes = new byte[intSet.size()*4];
        int byteIndex=0;
        for(int num: intSet) {
            bytes[byteIndex++] = (byte)((num>>24) & 0xff);
            bytes[byteIndex++] = (byte)((num>>16) & 0xff);
            bytes[byteIndex++] = (byte)((num>>8) & 0xff);
            bytes[byteIndex++] = (byte)(num & 0xff);
        }
        return bytes;
    }

    public static List<Integer> bytes2IntList(byte[] bytes) {
        List<Integer> numbers = new ArrayList<>();
        if (bytes == null || bytes.length < 4) {
            return numbers;
        }
        int byteIndex = 0;
        while (byteIndex < bytes.length) {
            int v = bytes[byteIndex++] & 0xff;
            v <<= 8;
            v |= (bytes[byteIndex++] & 0xff);
            v <<= 8;
            v |= (bytes[byteIndex++] & 0xff);
            v <<= 8;
            v |= (bytes[byteIndex++] & 0xff);
            numbers.add(v);
        }
        return numbers;
    }

    public static byte[] intList2Bytes(List<Integer> intSet) {
        byte[] bytes = new byte[intSet.size()*4];
        int byteIndex=0;
        for(int num: intSet) {
            bytes[byteIndex++] = (byte)((num>>24) & 0xff);
            bytes[byteIndex++] = (byte)((num>>16) & 0xff);
            bytes[byteIndex++] = (byte)((num>>8) & 0xff);
            bytes[byteIndex++] = (byte)(num & 0xff);
        }
        return bytes;
    }
}
