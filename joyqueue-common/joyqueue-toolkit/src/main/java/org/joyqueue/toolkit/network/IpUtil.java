/**
 * Copyright 2019 The JoyQueue Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.joyqueue.toolkit.network;

import sun.net.util.IPAddressUtil;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Ipv4工具
 *
 * @author hexiaofeng
 */
public class IpUtil {

    public static String IPV4_PORT_SEPARATOR = ":";
    public static String IPV6_PORT_SEPARATOR = "_";
    public static String IPV4_SEPARATOR = ".";
    public static String IPV6_SEPARATOR = ":";

    /**
     * 管理IP
     */
    public static String MANAGE_IP = "10.";
    /**
     * 网卡
     */
    public static String NET_INTERFACE;

    /**
     * 内网地址
     */
    public static final Lan INTRANET = new Lan("172.16.0.0/12;192.168.0.0/16;10.0.0.0/8");

    /**
     * 是否启用IPV6
     */
    public static boolean PREFER_IPV6 = false;

    public static String PREFER_HOSTNAME_OVERIP = "preferHostnameOverIp";

    public static String NODE_NAME_ENV = "NODE_NAME";

    static {
        // 从环境变量里面获取默认的网卡和管理网络
        NET_INTERFACE = System.getProperty("nic");
        MANAGE_IP = System.getProperty("manage_ip", MANAGE_IP);
        PREFER_IPV6 = Boolean.valueOf(System.getProperty("java.net.preferIPv6Stack", "false"));


    }

    /**
     * 得到本机所有的地址
     *
     * @return 本机所有的地址
     */
    public static List<String> getLocalIps() {
        return getLocalIps(null, null);
    }

    /**
     * 得到指定网卡上的地址
     *
     * @param nic     网卡
     * @param exclude 排除的地址
     */
    public static List<String> getLocalIps(final String nic, final String exclude) {
        try {
            List<String> result = new ArrayList<String>();
            NetworkInterface ni;
            Enumeration<InetAddress> ias;
            InetAddress address;
            Enumeration<NetworkInterface> netInterfaces = NetworkInterface.getNetworkInterfaces();
            while (netInterfaces.hasMoreElements()) {
                ni = netInterfaces.nextElement();
                if (nic != null && !nic.isEmpty() && !ni.getName().equals(nic)) {
                    continue;
                }
                ias = ni.getInetAddresses();
                while (ias.hasMoreElements()) {
                    address = ias.nextElement();
                    if (!address.isLoopbackAddress() &&
                            //回送地址：它是分配给回送接口的地址
                            !address.isAnyLocalAddress() &&
                            //多播地址：也称为 Anylocal 地址或通配符地址
                            ((address instanceof Inet4Address && !PREFER_IPV6) || (address instanceof Inet6Address && PREFER_IPV6))) {
                        result.add(address.getHostAddress());
                    }
                }
            }
            // 只有一个IP
            int count = result.size();
            if (count <= 1) {
                return result;
            }
            if (exclude != null && !exclude.isEmpty()) {
                String ip;
                // 多个IP，排除IP
                for (int i = count - 1; i >= 0; i--) {
                    ip = result.get(i);
                    if (ip.startsWith(exclude)) {
                        // 删除排除的IP
                        result.remove(i);
                        count--;
                        if (count == 1) {
                            // 确保有一个IP
                            break;
                        }
                    }
                }
            }

            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 得到本机内网地址
     *
     * @param nic      网卡
     * @param manageIp 管理段IP地址
     * @return 本机地址
     */
    public static String getLocalIp(final String nic, final String manageIp) {
        List<String> ips = getLocalIps(nic, manageIp);
        if (ips != null && !ips.isEmpty()) {
            if (ips.size() == 1) {
                return ips.get(0);
            }
            if (!PREFER_IPV6) {
                for (String ip : ips) {
                    if (INTRANET.contains(ip)) {
                        return ip;
                    }
                }
            }
            return ips.get(0);
        }
        return null;
    }

    /**
     * 得到本机内网地址
     *
     * @param manageIp 管理段IP地址
     * @return 本机地址
     */
    public static String getLocalIp(final String manageIp) {
        return getLocalIp(NET_INTERFACE, manageIp);
    }

    /**
     * 得到本机内网地址
     *
     * @return 本机地址
     */
    public static String getLocalIp() {
        // In JD.com IDC, this should be:
        // return getLocalIp(NET_INTERFACE, MANAGE_IP);

        boolean preferHostnameOverIp = Boolean.valueOf(System.getProperty(PREFER_HOSTNAME_OVERIP));
        if (preferHostnameOverIp) {
            return getDefaultLocalHost();
        } else {
            return getDefaultLocalIp();
        }
    }

    public static String getDefaultLocalHost() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return getDefaultLocalIp();
        }
    }

    public static String getDefaultLocalIp() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {

            List<String> ips = getLocalIps();
            if(null != ips && !ips.isEmpty()){
                return ips.get(0);
            }

            return "127.0.0.1";
        }
    }

    public static String getNode() {
        try {
            return System.getenv(NODE_NAME_ENV);
        } catch (Throwable e) {
            return "";
        }
    }
    /**
     * 把地址对象转换成字符串
     *
     * @param address 地址
     * @return 地址字符串
     */
    //TODO
    public static String toAddress(final SocketAddress address) {
        if (address == null) {
            return null;
        }
        if (address instanceof InetSocketAddress) {
            InetSocketAddress isa = (InetSocketAddress) address;
            StringBuilder builder = new StringBuilder(50);
            if (address instanceof InetSocketAddress) {
                builder.append(isa.getAddress().getHostAddress());
                String separator = isValidIpV4Address(((InetSocketAddress) address).getHostString()) ? IPV4_PORT_SEPARATOR : IPV6_PORT_SEPARATOR;
                builder.append(separator).append(isa.getPort());
            }
            return builder.toString();
        } else {
            return address.toString();
        }
    }

    /**
     * 从地址数组中获取IP，跳过必要的端口
     *
     * @param address 地址数组
     * @return IP
     */
    public static String toIp(final byte[] address) {
        if (address == null || address.length < 4) {
            return null;
        }
        int pos = 0;
        if (address.length == 6 || address.length == 18) {
            // 跳过端口
            pos += 2;
        }
        StringBuilder builder = new StringBuilder();
        for (int i = pos; i < address.length - 1; i++) {
            if (address.length <= 6) {
                builder.append(address[pos++] & 0xFF).append(IPV4_SEPARATOR);
            } else {
                builder = new StringBuilder(20);
                builder.append(address[pos++] & 0xFF).append(IPV6_SEPARATOR);
            }
        }

        builder.append(address[pos++] & 0xFF);

        return builder.toString();
    }


    /**
     * 把地址转化成字节数组，如果有端口，则第一和第二字节为端口，其余为IP段
     * <p>
     * <p>
     * 解析地址<br>，分隔符支持".",":","_"
     * IPV4地址第1-4个元素为IP段，第5个元素为端口，如果第5个元素为-1，则表示端口不存在
     * IPV6地址第1-8个元素为IP段，第9个元素为端口，如果第9个元素为-1，则表示端口不存在
     *
     * @param address 地址
     * @return 字节数组
     */
    public static byte[] toByte(String address) {
        if (address == null || address.isEmpty()) {
            return null;
        }

        String upperAddress = address.toUpperCase();
        StringBuilder host = new StringBuilder();
        char[] chars = upperAddress.toCharArray();
        char ch;
        int start = -1;
        int end = -1;
        int index = 0;
        char sep = ' ';
        for (int i = 0; i < chars.length; i++) {
            ch = chars[i];
            switch (ch) {
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                case 'A':
                case 'B':
                case 'C':
                case 'D':
                case 'E':
                case 'F':
                    if (start == -1) {
                        start = i;
                    }
                    end = i;
                    break;
                case '_':
                case ':':
                case '.':
                    index++;
                    // 分隔符
                    if (start == -1) {
                        // 前面必须有字符
                        return null;
                    }
                    if (start != 0) {
                        host.append(sep);
                    }
                    host.append(new String(chars, start, end - start + 1));
                    sep = ch;
                    start = -1;
                    end = -1;
                    break;
                default:
                    return null;
            }
        }
        String last = new String(chars, start, end - start + 1);
        if (index == 3 || index == 7) {
            host.append(String.valueOf(sep)).append(last);
            return toByteWithoutPort(host.toString());
        } else {
            byte[] data = toByteWithoutPort(host.toString());
            int port = Integer.valueOf(new String(chars, start, end - start + 1));
            byte[] result = new byte[data.length + 2];

            System.arraycopy(data, 0, result, 2, data.length);
            result[1] = (byte) (port >> 8 & 0xFF);
            result[0] = (byte) (port & 0xFF);
            return result;
        }
    }


    /**
     * 地址转化成字节数组
     *
     * @param socketAddress 地址对象
     * @return 字节数组
     */
    public static byte[] toByte(InetSocketAddress socketAddress) {
        if (socketAddress == null) {
            throw new IllegalArgumentException("socketAddress is null");
        }
        InetAddress inetAddress = socketAddress.getAddress();
        if (inetAddress == null) {
            throw new IllegalArgumentException("socketAddress is invalid");
        }
        byte[] address = inetAddress.getAddress();
        byte[] result = new byte[address.length + 2];
        System.arraycopy(address, 0, result, 2, address.length);
        int port = socketAddress.getPort();
        result[1] = (byte) (port >> 8 & 0xFF);
        result[0] = (byte) (port & 0xFF);
        return result;
    }

    /**
     * 把字节数组转换成地址对象
     *
     * @param address 地址字节数组
     * @return 地址对象
     */
    public static InetSocketAddress toAddress(final byte[] address) {
        if (address == null || (address.length != 6 && address.length != 18)) {
            // 端口2个字节，IPV4 4字节，IPV6 16字节
            throw new IllegalArgumentException("address is invalid");
        }
        // 低位2个字节是端口数据
        int port = address[0] & 0xFF;
        port |= (address[1] << 8 & 0xFF00);

        try {
            InetAddress addr = InetAddress.getByAddress(null, Arrays.copyOfRange(address, 2, address.length));
            return new InetSocketAddress(addr, port);
        } catch (UnknownHostException ignored) {
            return null;
        }
    }


    /**
     * 把地址数组转换成字符串
     *
     * @param address 字节数组
     * @param builder 字符串构造器
     */
    public static void toAddress(byte[] address, StringBuilder builder) {
        if (builder == null) {
            return;
        }
        if (address == null) {
            throw new IllegalArgumentException("address is invalid");
        }
        if (address.length < 4) {
            throw new IllegalArgumentException("address is invalid");
        }
        int pos = 0;
        int port = 0;
        if (address.length >= 6) {
            port = address[pos++] & 0xFF;
            port |= (address[pos++] << 8 & 0xFF00);
        }
        builder.append(address[pos++] & 0xFF).append('.');
        builder.append(address[pos++] & 0xFF).append('.');
        builder.append(address[pos++] & 0xFF).append('.');
        builder.append(address[pos++] & 0xFF);
        if (address.length >= 6) {
            builder.append(':').append(port);
        }
    }


    /**
     * 分解IP,只支持IPV4
     *
     * @param ip ip地址
     * @return 分段
     */
    public static int[] parseIp(final String ip) {
        if (!isValidIpV4Address(ip)) {
            return null;
        }

        if (ip == null || ip.isEmpty()) {
            return null;
        }
        int[] parts = new int[4];
        int index = 0;
        int start = -1;
        int end = -1;
        int part;
        char[] chars = ip.toCharArray();
        char ch = 0;
        for (int i = 0; i < chars.length; i++) {
            if (index > 3) {
                // 超过了4个数字
                return null;
            }
            ch = chars[i];
            switch (ch) {
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                    if (start == -1) {
                        start = i;
                    }
                    end = i;
                    if (end - start > 2) {
                        // 超长了，最多3个数字
                        return null;
                    }
                    break;
                case '.':
                    // 分隔符
                    if (start == -1) {
                        // 前面必须有字符
                        return null;
                    }
                    part = Integer.parseInt(new String(chars, start, end - start + 1));
                    if (part > 255) {
                        return null;
                    }
                    parts[index++] = part;
                    start = -1;
                    end = -1;
                    break;
                default:
                    return null;
            }
        }
        if (start > -1) {
            part = Integer.parseInt(new String(chars, start, end - start + 1));
            if (part > 255) {
                return null;
            }
            parts[index] = part;
            return index == 3 ? parts : null;
        } else {
            // 以.结尾
            return null;
        }
    }

    /**
     * 把IP地址转换成长整型，只支持IPv4
     *
     * @param ip IP地址
     * @return 长整形
     */
    public static long toLong(final String ip) {
        int[] data = parseIp(ip);
        if (data == null) {
            throw new IllegalArgumentException(String.format("invalid ip %s", ip));
        }
        long result = 0;
        result += ((long) data[0]) << 24;
        result += ((long) (data[1]) << 16);
        result += ((long) (data[2]) << 8);
        result += ((long) (data[3]));
        return result;
    }

    /**
     * 把长整形转换成IP地址，只支持IPv4
     *
     * @param ip 长整型
     * @return IP字符串
     */
    public static String toIp(long ip) {
        StringBuffer sb = new StringBuffer(20);
        long part1 = (ip & 0xFFFFFFFF) >>> 24;
        long part2 = (ip & 0x00FFFFFF) >>> 16;
        long part3 = (ip & 0x0000FFFF) >>> 8;
        long part4 = ip & 0x000000FF;
        //直接右移24位
        sb.append(part1).append('.');
        //将高8位置0，然后右移16位
        sb.append(part2).append('.');
        //将高16位置0，然后右移8位
        sb.append(part3).append('.');
        //将高24位置0
        sb.append(part4);
        return sb.toString();
    }


    public static void main(String[] args) throws Exception {
        System.setProperty("java.net.preferIPv6Stack", "true");

        List<String> ips = getLocalIps();

        ips.forEach(ip -> System.out.println(ip));


        String ip = getLocalIp();
        System.out.println("local ip :" + ip);


        String v6Str = "fe80:0:0:0:10af:7794:db80:1ba1";
        String v4Str = "127.0.0.1";
        System.out.println(IPAddressUtil.isIPv4LiteralAddress(v6Str));
        System.out.println(IPAddressUtil.isIPv6LiteralAddress(v4Str));
        byte[] v4Byte = IPAddressUtil.textToNumericFormatV4(v4Str);
        byte[] v6Byte = IPAddressUtil.textToNumericFormatV6(v6Str);
        StringBuilder buffer = new StringBuilder();
        IpUtil.toAddress(v4Byte, buffer);
        System.out.println("buffer_v4:" + buffer.toString());
        buffer = new StringBuilder();
        IpUtil.toAddress(v6Byte, buffer);
        System.out.println("hex_v6:" + Integer.toHexString(v6Byte[0] & 0xFF).toUpperCase());
        System.out.println("hex_v6:" + Integer.toHexString(v6Byte[1] & 0xFF).toUpperCase());
        System.out.println("buffer_v6:" + buffer.toString());

        InetSocketAddress address = new InetSocketAddress(v6Str, 80);
        v6Byte = address.getAddress().getAddress();
        System.out.println("hex_v6_1:" + Integer.toHexString(v6Byte[0] & 0xFF).toUpperCase());

    }


    /**
     * Creates an byte[] based on an ipAddressString. No error handling is
     * performed here.
     */
    private static byte[] toByteWithoutPort(String ipAddressString) {

        if (isValidIpV4Address(ipAddressString)) {
            StringTokenizer tokenizer = new StringTokenizer(ipAddressString, ".");
            String token;
            int tempInt;
            byte[] byteAddress = new byte[4];
            for (int i = 0; i < 4; i++) {
                token = tokenizer.nextToken();
                tempInt = Integer.parseInt(token);
                byteAddress[i] = (byte) tempInt;
            }

            return byteAddress;
        }

        if (isValidIpV6Address(ipAddressString)) {
            if (ipAddressString.charAt(0) == '[') {
                ipAddressString = ipAddressString.substring(1, ipAddressString.length() - 1);
            }

            int percentPos = ipAddressString.indexOf('%');
            if (percentPos >= 0) {
                ipAddressString = ipAddressString.substring(0, percentPos);
            }

            StringTokenizer tokenizer = new StringTokenizer(ipAddressString, ":.", true);
            ArrayList<String> hexStrings = new ArrayList<String>();
            ArrayList<String> decStrings = new ArrayList<String>();
            String token = "";
            String prevToken = "";
            int doubleColonIndex = -1; // If a double colon exists, we need to
            // insert 0s.

            // Go through the tokens, including the seperators ':' and '.'
            // When we hit a : or . the previous token will be added to either
            // the hex list or decimal list. In the case where we hit a ::
            // we will save the index of the hexStrings so we can add zeros
            // in to fill out the string
            while (tokenizer.hasMoreTokens()) {
                prevToken = token;
                token = tokenizer.nextToken();

                if (":".equals(token)) {
                    if (":".equals(prevToken)) {
                        doubleColonIndex = hexStrings.size();
                    } else if (!prevToken.isEmpty()) {
                        hexStrings.add(prevToken);
                    }
                } else if (".".equals(token)) {
                    decStrings.add(prevToken);
                }
            }

            if (":".equals(prevToken)) {
                if (":".equals(token)) {
                    doubleColonIndex = hexStrings.size();
                } else {
                    hexStrings.add(token);
                }
            } else if (".".equals(prevToken)) {
                decStrings.add(token);
            }

            // figure out how many hexStrings we should have
            // also check if it is a IPv4 address
            int hexStringsLength = 8;

            // If we have an IPv4 address tagged on at the end, subtract
            // 4 bytes, or 2 hex words from the total
            if (!decStrings.isEmpty()) {
                hexStringsLength -= 2;
            }

            // if we hit a double Colon add the appropriate hex strings
            if (doubleColonIndex != -1) {
                int numberToInsert = hexStringsLength - hexStrings.size();
                for (int i = 0; i < numberToInsert; i++) {
                    hexStrings.add(doubleColonIndex, "0");
                }
            }

            byte[] ipByteArray = new byte[16];

            // Finally convert these strings to bytes...
            for (int i = 0; i < hexStrings.size(); i++) {
                convertToBytes(hexStrings.get(i), ipByteArray, i * 2);
            }

            // Now if there are any decimal values, we know where they go...
            for (int i = 0; i < decStrings.size(); i++) {
                ipByteArray[i + 12] = (byte) (Integer.parseInt(decStrings.get(i)) & 255);
            }
            return ipByteArray;
        }
        return null;
    }

    /**
     * Converts a 4 character hex word into a 2 byte word equivalent
     */
    private static void convertToBytes(String hexWord, byte[] ipByteArray, int byteIndex) {

        int hexWordLength = hexWord.length();
        int hexWordIndex = 0;
        ipByteArray[byteIndex] = 0;
        ipByteArray[byteIndex + 1] = 0;
        int charValue;

        // high order 4 bits of first byte
        if (hexWordLength > 3) {
            charValue = getIntValue(hexWord.charAt(hexWordIndex++));
            ipByteArray[byteIndex] |= charValue << 4;
        }

        // low order 4 bits of the first byte
        if (hexWordLength > 2) {
            charValue = getIntValue(hexWord.charAt(hexWordIndex++));
            ipByteArray[byteIndex] |= charValue;
        }

        // high order 4 bits of second byte
        if (hexWordLength > 1) {
            charValue = getIntValue(hexWord.charAt(hexWordIndex++));
            ipByteArray[byteIndex + 1] |= charValue << 4;
        }

        // low order 4 bits of the first byte
        charValue = getIntValue(hexWord.charAt(hexWordIndex));
        ipByteArray[byteIndex + 1] |= charValue & 15;
    }

    static int getIntValue(char c) {

        switch (c) {
            case '0':
                return 0;
            case '1':
                return 1;
            case '2':
                return 2;
            case '3':
                return 3;
            case '4':
                return 4;
            case '5':
                return 5;
            case '6':
                return 6;
            case '7':
                return 7;
            case '8':
                return 8;
            case '9':
                return 9;
        }

        c = Character.toLowerCase(c);
        switch (c) {
            case 'a':
                return 10;
            case 'b':
                return 11;
            case 'c':
                return 12;
            case 'd':
                return 13;
            case 'e':
                return 14;
            case 'f':
                return 15;
        }
        return 0;
    }

    public static boolean isValidIpV6Address(String ipAddress) {
        int length = ipAddress.length();
        boolean doubleColon = false;
        int numberOfColons = 0;
        int numberOfPeriods = 0;
        int numberOfPercent = 0;
        StringBuilder word = new StringBuilder();
        char c = 0;
        char prevChar;
        int offset = 0; // offset for [] ip addresses

        if (length < 2) {
            return false;
        }

        for (int i = 0; i < length; i++) {
            prevChar = c;
            c = ipAddress.charAt(i);
            switch (c) {

                // case for an open bracket [x:x:x:...x]
                case '[':
                    if (i != 0) {
                        return false; // must be first character
                    }
                    if (ipAddress.charAt(length - 1) != ']') {
                        return false; // must have a close ]
                    }
                    offset = 1;
                    if (length < 4) {
                        return false;
                    }
                    break;

                // case for a closed bracket at end of IP [x:x:x:...x]
                case ']':
                    if (i != length - 1) {
                        return false; // must be last charcter
                    }
                    if (ipAddress.charAt(0) != '[') {
                        return false; // must have a open [
                    }
                    break;

                // case for the last 32-bits represented as IPv4 x:x:x:x:x:x:d.d.d.d
                case '.':
                    numberOfPeriods++;
                    if (numberOfPeriods > 3) {
                        return false;
                    }
                    if (!isValidIp4Word(word.toString())) {
                        return false;
                    }
                    if (numberOfColons != 6 && !doubleColon) {
                        return false;
                    }
                    // a special case ::1:2:3:4:5:d.d.d.d allows 7 colons with an
                    // IPv4 ending, otherwise 7 :'s is bad
                    if (numberOfColons == 7 && ipAddress.charAt(offset) != ':' &&
                            ipAddress.charAt(1 + offset) != ':') {
                        return false;
                    }
                    word.delete(0, word.length());
                    break;

                case ':':
                    // FIX "IP6 mechanism syntax #ip6-bad1"
                    // An IPV6 address cannot start with a single ":".
                    // Either it can starti with "::" or with a number.
                    if (i == offset && (ipAddress.length() <= i || ipAddress.charAt(i + 1) != ':')) {
                        return false;
                    }
                    // END FIX "IP6 mechanism syntax #ip6-bad1"
                    numberOfColons++;
                    if (numberOfColons > 7) {
                        return false;
                    }
                    if (numberOfPeriods > 0) {
                        return false;
                    }
                    if (prevChar == ':') {
                        if (doubleColon) {
                            return false;
                        }
                        doubleColon = true;
                    }
                    word.delete(0, word.length());
                    break;
                case '%':
                    if (numberOfColons == 0) {
                        return false;
                    }
                    numberOfPercent++;

                    // validate that the stuff after the % is valid
                    if (i + 1 >= length) {
                        // in this case the percent is there but no number is
                        // available
                        return false;
                    }
                    try {
                        if (Integer.parseInt(ipAddress.substring(i + 1)) < 0) {
                            return false;
                        }
                    } catch (NumberFormatException e) {
                        // right now we just support an integer after the % so if
                        // this is not
                        // what is there then return
                        return false;
                    }
                    break;

                default:
                    if (numberOfPercent == 0) {
                        if (word != null && word.length() > 3) {
                            return false;
                        }
                        if (!isValidHexChar(c)) {
                            return false;
                        }
                    }
                    word.append(c);
            }
        }

        // Check if we have an IPv4 ending
        if (numberOfPeriods > 0) {
            // There is a test case with 7 colons and valid ipv4 this should resolve it
            if (numberOfPeriods != 3 || !(isValidIp4Word(word.toString()) && numberOfColons < 7)) {
                return false;
            }
        } else {
            // If we're at then end and we haven't had 7 colons then there is a
            // problem unless we encountered a doubleColon
            if (numberOfColons != 7 && !doubleColon) {
                return false;
            }

            // If we have an empty word at the end, it means we ended in either
            // a : or a .
            // If we did not end in :: then this is invalid
            if (numberOfPercent == 0) {
                if (word.length() == 0 && ipAddress.charAt(length - 1 - offset) == ':' &&
                        ipAddress.charAt(length - 2 - offset) != ':') {
                    return false;
                }
            }
        }

        return true;
    }

    public static boolean isValidIp4Word(String word) {
        char c;
        if (word.length() < 1 || word.length() > 3) {
            return false;
        }
        for (int i = 0; i < word.length(); i++) {
            c = word.charAt(i);
            if (!(c >= '0' && c <= '9')) {
                return false;
            }
        }
        return Integer.parseInt(word) <= 255;
    }

    static boolean isValidHexChar(char c) {
        return c >= '0' && c <= '9' || c >= 'A' && c <= 'F' || c >= 'a' && c <= 'f';
    }

    /**
     * Takes a string and parses it to see if it is a valid IPV4 address.
     *
     * @return true, if the string represents an IPV4 address in dotted
     * notation, false otherwise
     */
    public static boolean isValidIpV4Address(String value) {

        int periods = 0;
        int i;
        int length = value.length();

        if (length > 15) {
            return false;
        }
        char c;
        StringBuilder word = new StringBuilder();
        for (i = 0; i < length; i++) {
            c = value.charAt(i);
            if (c == '.') {
                periods++;
                if (periods > 3) {
                    return false;
                }
                if (word.length() == 0) {
                    return false;
                }
                if (Integer.parseInt(word.toString()) > 255) {
                    return false;
                }
                word.delete(0, word.length());
            } else if (!Character.isDigit(c)) {
                return false;
            } else {
                if (word.length() > 2) {
                    return false;
                }
                word.append(c);
            }
        }

        if (word.length() == 0 || Integer.parseInt(word.toString()) > 255) {
            return false;
        }

        return periods == 3;
    }

    public static long ip2Long(final String ip) {
        String[] ipItem = ip.split("\\.");
        assert ipItem.length == 4;
        long[] ipNum = new long[4];
        ipNum[0] = Long.parseLong(ipItem[0]);
        ipNum[1] = Long.parseLong(ipItem[1]);
        ipNum[2] = Long.parseLong(ipItem[2]);
        ipNum[3] = Long.parseLong(ipItem[3]);
        return (ipNum[0] << 24) + (ipNum[1] << 16) + (ipNum[2] << 8) + ipNum[3];
    }

    public static String long2Ip(final long num) {
        return (num >>> 24) +
                "." +
                ((num & 0x00FFFFFF) >>> 16) +
                "." +
                ((num & 0x0000FFFF) >>> 8) +
                "." +
                (num & 0x000000FF);
    }


}
