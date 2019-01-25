package me.fiery.AudioEditor;

import javax.sound.sampled.AudioFormat;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

class BytesUtil {

    static ArrayList<short[]> pair(AudioFormat audioFormat, ArrayList<byte[]> array) {
        ArrayList<short[]> result = new ArrayList<>();
        int bytesPerFrame = (audioFormat.getSampleSizeInBits() / 8) * audioFormat.getChannels();
        int pairSize = bytesPerFrame / audioFormat.getChannels();

        for (byte[] buffer : array) {
            short[] bytesPaired = new short[buffer.length / pairSize];
            int j = 0;
            for (int i = 0; i < buffer.length; i += pairSize) {
                byte[] bytesPair = new byte[pairSize];
                System.arraycopy(buffer, i, bytesPair, 0, pairSize);
                bytesPaired[j++] = ByteBuffer
                        .wrap(bytesPair)
                        .order(audioFormat.isBigEndian() ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN)
                        .getShort();
            }
            result.add(bytesPaired);
        }

        return result;
    }

    static ArrayList<byte[]> sever(AudioFormat audioFormat, ArrayList<short[]> array) {
        ArrayList<byte[]> result = new ArrayList<>();
        int bytesPerFrame = (audioFormat.getSampleSizeInBits() / 8) * audioFormat.getChannels();
        int pairSize = bytesPerFrame / audioFormat.getChannels();

        for (short[] buffer : array) {
            byte[] bytesSevered = new byte[buffer.length * pairSize];
            int i = 0;
            for (short pair : buffer) {
                byte[] bytesPair = new byte[pairSize];
                ByteBuffer
                        .wrap(bytesPair)
                        .order(audioFormat.isBigEndian() ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN)
                        .putShort(pair);
                for (byte bytesPairByte : bytesPair)
                    bytesSevered[i++] = bytesPairByte;
            }
            result.add(bytesSevered);
        }

        return result;
    }

}