package com.ble.devcie;

/**
 * Created by jhc on 2018/7/16.
 */

public class Prot {
    static final byte PKT_BEGIN = (byte) 0x6f;
    static final byte PKT_END = (byte) 0x8f;

    public static final byte PKT_DATA_SET = (byte) 0x71;
    public static final byte PKT_DATA_SET_RSP = (byte) 0x81;
    public static final byte PKT_DATA_GET = (byte) 0x70;
    public static final byte PKT_DATA_GET_RSP = (byte) 0x71;

    static final int PKT_MIN_LENGHT = 6;

    static final int PKT_SUCCESS = 0;
    static final int PKT_LENGHT_NOENOUGH = -1;



    byte[] pkt_content;
    int buff_begin;

    public static int tryUnpack(byte[] pktBuff, int parseOffset, Prot result){

        if(parseOffset > pktBuff.length-1)
            return PKT_LENGHT_NOENOUGH;

        for(; parseOffset<pktBuff.length; parseOffset++) {
            if (pktBuff[parseOffset] == PKT_BEGIN)
                break;
        }

        int parseLen = pktBuff.length-parseOffset;
        if(parseLen < PKT_MIN_LENGHT)
            return PKT_LENGHT_NOENOUGH;

        int datLen = pktBuff[parseOffset+4] << 8 | pktBuff[parseOffset+3];
        if(!((parseLen >= PKT_MIN_LENGHT+datLen) && (pktBuff[parseOffset+datLen+PKT_MIN_LENGHT-1] == PKT_END))){
            return tryUnpack(pktBuff,parseOffset+1, result);
        }

        result.buff_begin = parseOffset;
        result.pkt_content = new byte[PKT_MIN_LENGHT + datLen];
        for(int i=0; i<PKT_MIN_LENGHT + datLen; i++){
            result.pkt_content[i] = pktBuff[parseOffset + i];
        }

        return PKT_SUCCESS;
    }

    public static byte[] pack(byte cmd, byte dir, byte[] dat) {
        int datLen = (dat == null) ? 0 : dat.length;
        byte[] pktBuff = new byte[PKT_MIN_LENGHT + datLen];

        pktBuff[0] = PKT_BEGIN;
        pktBuff[1] = cmd;
        pktBuff[2] = dir;
        pktBuff[3] = (byte) (datLen&0xff);
        pktBuff[4] = (byte) ((datLen>>8)&0xff);
        int i = 0;
        for(; i<datLen; i++){
            pktBuff[5+i] = dat[0];
        }
        pktBuff[5+i] = PKT_END;

        return pktBuff;
    }
}
