package me.fiery.AudioEditor;

import java.util.ArrayList;

class AudioEffects {

    // Volume manipulation
    // Reduce volume of one channel by a percentage, back and forth
    private static ArrayList<short[]> volumeManipulation(ArrayList<short[]> arrayList, float frameRate) {
        final double DURATION = 3.0; // time in seconds

        double progressStep = 1.0 / (frameRate * DURATION);
        double progress = 0.0;

        boolean left = true;
        boolean stay = true;

        for (short[] array : arrayList) {
            for (int i = 0; i < array.length; i += 2) {
                int source = left ? i : (i + 1);
                int target = left ? (i + 1) : i;

                double multiplier = stay ? 1.0 : progress;
                array[source] = (short) (array[source] * multiplier);
                array[target] = (short) (array[target] * (1.0 - multiplier));

                progress += progressStep;

                if (progress > 1.0) {
                    if (stay) {
                        left = !left;
                        stay = false;
                    } else {
                        stay = true;
                    }
                    progress = 0.0;
                }
            }
        }

        return arrayList;
    }

    // Channel mixing
    // Average value of both channels before doing volume manipulation
    static ArrayList<short[]> channelMixing(ArrayList<short[]> arrayList, float frameRate) {
        for (short[] array : arrayList) {
            for (int i = 0; i < array.length; i += 2) {
                short avg = (short) ((array[i] + array[i + 1]) * 0.5);
                array[i] = array[i + 1] = avg;
            }
        }

        return volumeManipulation(arrayList, frameRate);
    }
}
