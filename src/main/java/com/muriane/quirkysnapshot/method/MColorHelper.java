package com.muriane.quirkysnapshot.method;

import java.awt.*;

public class MColorHelper {
    public static Color HSVtoRGB(float[] hsv) {
        float[] rgb = new float[3];
        float hue = hsv[0];
        float saturation = hsv[1];
        float value = hsv[2];

        rgb[0] = value;
        rgb[1] = value;
        rgb[2] = value;

        if (saturation > 0.0f) {
            hue = (hue < 1.0f) ? hue * 6.0f : 0.0f;
            int integer = (int) hue;
            float f = hue - (float) integer;
            switch (integer) {
                case 0:
                    rgb[1] *= 1.0f - saturation * (1.0f - f);
                    rgb[2] *= 1.0f - saturation;
                    break;
                case 1:
                    rgb[0] *= 1.0f - saturation * f;
                    rgb[2] *= 1.0f - saturation;
                    break;
                case 2:
                    rgb[0] *= 1.0f - saturation;
                    rgb[2] *= 1.0f - saturation * (1.0f - f);
                    break;
                case 3:
                    rgb[0] *= 1.0f - saturation;
                    rgb[1] *= 1.0f - saturation * f;
                    break;
                case 4:
                    rgb[0] *= 1.0f - saturation * (1.0f - f);
                    rgb[1] *= 1.0f - saturation;
                    break;
                case 5:
                    rgb[1] *= 1.0f - saturation;
                    rgb[2] *= 1.0f - saturation * f;
                    break;
            }
        }
        return new Color(rgb[0], rgb[1], rgb[2]);
    }
}
