package me.fiery.AudioEditor;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

class BytesUtil {

    private static ByteBuffer setByteBufferOrder(ByteBuffer byteBuffer, boolean isBigEndian) {
        if (isBigEndian)
            return byteBuffer.order(ByteOrder.BIG_ENDIAN);
        else
            return byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
    }

    static ArrayList<short[]> pair(ArrayList<byte[]> array, int bit, int channel, boolean isBigEndian) {
        ArrayList<short[]> result = new ArrayList<>();
        int bytesPerFrame = (bit / 8) * channel;
        int pairSize = bytesPerFrame / channel;
        for (byte[] buffer : array) {
            short[] bytesPaired = new short[buffer.length / pairSize];
            int j = 0;
            for (int i = 0; i < buffer.length; i += pairSize) {
                byte[] bytesPair = new byte[pairSize];
                System.arraycopy(buffer, i, bytesPair, 0, pairSize);
                ByteBuffer byteBuffer = ByteBuffer.wrap(bytesPair);
                byteBuffer = setByteBufferOrder(byteBuffer, isBigEndian);
                bytesPaired[j++] = byteBuffer.getShort();
            }
            result.add(bytesPaired);
        }
        return result;
    }

    static ArrayList<byte[]> sever(ArrayList<short[]> array, int bit, int channel, boolean isBigEndian) {
        ArrayList<byte[]> result = new ArrayList<>();
        int bytesPerFrame = (bit / 8) * channel;
        int pairSize = bytesPerFrame / channel;
        for (short[] buffer : array) {
            byte[] bytesSevered = new byte[buffer.length * pairSize];
            int i = 0;
            for (short pair : buffer) {
                byte[] bytesPair = new byte[pairSize];
                ByteBuffer byteBuffer = ByteBuffer.wrap(bytesPair);
                byteBuffer = setByteBufferOrder(byteBuffer, isBigEndian);
                byteBuffer.putShort(pair);
                for (byte bytesPairByte : bytesPair)
                    bytesSevered[i++] = bytesPairByte;
            }
            result.add(bytesSevered);
        }
        return result;
    }

}