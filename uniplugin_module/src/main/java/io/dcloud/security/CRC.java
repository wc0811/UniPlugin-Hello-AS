package io.dcloud.security;

public class CRC {

    public static byte[] calcCRC(byte[] pByte) {

        int sum = 0;
        for (int i = 0;i< pByte.length - 1;i++){
            sum = sum + (0x000000ff&pByte[i]);
        }
        pByte[pByte.length - 1] = (byte) sum;
        return pByte;
    }

    public static boolean vertifyCRC(byte[] data){
        int sum = 0;
        for (int i = 0;i< data.length - 1;i++){
            sum = sum + (0x000000ff&data[i]);
        }
        if((byte) sum == data[data.length - 1]){
            return true;
        }
        return false;
    }

}
