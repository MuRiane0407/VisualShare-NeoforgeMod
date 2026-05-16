package com.muriane.quirkysnapshot;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.awt.*;

public class Config {
    public static final ModConfigSpec serverSpec;
    public static final Server SERVER;
    public static final ModConfigSpec clientSpec;
    public static final Client CLIENT;

    static {
        final Pair<Server, ModConfigSpec> specServerPair = new ModConfigSpec.Builder().configure(Server::new);
        serverSpec = specServerPair.getRight();
        SERVER = specServerPair.getLeft();
        final Pair<Client, ModConfigSpec> specClientPair = new ModConfigSpec.Builder().configure(Client::new);
        clientSpec = specClientPair.getRight();
        CLIENT = specClientPair.getLeft();
    }

    public static class Server {
        public final ModConfigSpec.EnumValue<DataType> FAST_SCREENSHOT_SHARE_DATA_TYPE;
        public final ModConfigSpec.IntValue FAST_SCREENSHOT_SHARE_MAX_SIZE;

        Server(ModConfigSpec.Builder builder){
            builder.push("fast_screenshot");
            FAST_SCREENSHOT_SHARE_DATA_TYPE = builder
                    .translation("quirkysnapshot.configuration.fast_screenshot.share.data_type")
                    .defineEnum("fast_screenshot.share.data_type", DataType.PNG);
            FAST_SCREENSHOT_SHARE_MAX_SIZE = builder
                    .translation("quirkysnapshot.configuration.fast_screenshot.share.max_size")
                    .defineInRange("fast_screenshot.share.max_size", 512, -1, 32768);
            builder.pop();
        }

        public enum DataType{
            PNG("png");

            private final String type;

            DataType(String type) {
                this.type = type;
            }

            public String getType(){
                return this.type;
            }
        }
    }

    public static class Client {
        public final ModConfigSpec.EnumValue<Client.InterpolationAlgorithm> FAST_SCREENSHOT_SHARE_INTERPOLATION_ALGORITHM;
        public final ModConfigSpec.DoubleValue FAST_SCREENSHOT_SHARE_THUMBNAIL_IMAGE_SIZE;
        public final ModConfigSpec.DoubleValue FAST_SCREENSHOT_SHARE_FULL_IMAGE_SIZE;

        Client(ModConfigSpec.Builder builder){
            builder.push("fast_screenshot");
            FAST_SCREENSHOT_SHARE_INTERPOLATION_ALGORITHM = builder
                    .translation("quirkysnapshot.configuration.fast_screenshot.share.interpolation_algorithm")
                    .defineEnum("fast_screenshot.share.interpolation_algorithm", Client.InterpolationAlgorithm.NEAREST);
            FAST_SCREENSHOT_SHARE_THUMBNAIL_IMAGE_SIZE = builder
                    .translation("quirkysnapshot.configuration.fast_screenshot.share.thumbnail_image_size")
                    .defineInRange("fast_screenshot.share.thumbnail_image_size", 0.25, 0, 1);
            FAST_SCREENSHOT_SHARE_FULL_IMAGE_SIZE = builder
                    .translation("quirkysnapshot.configuration.fast_screenshot.share.full_image_size")
                    .defineInRange("fast_screenshot.share.full_image_size", 0.75, 0, 1);
        }

        public enum InterpolationAlgorithm{
            NEAREST(Image.SCALE_REPLICATE),
            BILINEAR(Image.SCALE_AREA_AVERAGING);

            private final int algorithm;

            InterpolationAlgorithm(int algorithm) {
                this.algorithm = algorithm;
            }

            public int getAlgorithm() {
                return algorithm;
            }
        }
    }
}
