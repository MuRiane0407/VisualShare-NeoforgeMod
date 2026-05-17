package com.muriane.visual_share;

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
        public final ModConfigSpec.EnumValue<OverrideMode> FAST_SCREENSHOT_SHARE_OVERRIDE_CLIENT_PARAM;
        public final ModConfigSpec.EnumValue<DataType> FAST_SCREENSHOT_SHARE_DATA_TYPE;
        public final ModConfigSpec.IntValue FAST_SCREENSHOT_SHARE_AVIF_QUALITY;
        public final ModConfigSpec.IntValue FAST_SCREENSHOT_SHARE_AVIF_SPEED;
        public final ModConfigSpec.BooleanValue FAST_SCREENSHOT_SHARE_AVIF_LOSSLESS;
        public final ModConfigSpec.EnumValue<InterpolationAlgorithm> FAST_SCREENSHOT_SHARE_INTERPOLATION_ALGORITHM;
        public final ModConfigSpec.IntValue FAST_SCREENSHOT_SHARE_MAX_SIZE;
        public final ModConfigSpec.ConfigValue<String> FAST_SCREENSHOT_SHARE_PREFIX;
        public final ModConfigSpec.ConfigValue<String> FAST_SCREENSHOT_SHARE_SUBFIX;

        Server(ModConfigSpec.Builder builder){
            builder.push("fast_screenshot");
            builder.push("share");
            FAST_SCREENSHOT_SHARE_OVERRIDE_CLIENT_PARAM = builder
                    .translation("visual_share.configuration.fast_screenshot.share.override_client_param")
                    .defineEnum("override_client_param", OverrideMode.MAX);

            builder.push("override_share_param");
            FAST_SCREENSHOT_SHARE_DATA_TYPE = builder
                    .translation("visual_share.configuration.fast_screenshot.share_data_type")
                    .defineEnum("data_type", DataType.avif);

            builder.push("share_data_param");
            FAST_SCREENSHOT_SHARE_AVIF_QUALITY = builder
                    .translation("visual_share.configuration.fast_screenshot.share_data_param.avif_quality")
                    .defineInRange("avif_quality", 75, 0, 100);
            FAST_SCREENSHOT_SHARE_AVIF_SPEED = builder
                    .translation("visual_share.configuration.fast_screenshot.share_data_param.avif_speed")
                    .defineInRange("avif_speed", 10, 0, 10);
            FAST_SCREENSHOT_SHARE_AVIF_LOSSLESS = builder
                    .translation("visual_share.configuration.fast_screenshot.share_data_param.avif_lossless")
                    .define("avif_lossless", false);
            builder.pop();

            FAST_SCREENSHOT_SHARE_INTERPOLATION_ALGORITHM = builder
                    .translation("visual_share.configuration.fast_screenshot.share_interpolation_algorithm")
                    .defineEnum("interpolation_algorithm", InterpolationAlgorithm.Nearest);
            builder.pop();

            FAST_SCREENSHOT_SHARE_MAX_SIZE = builder
                    .translation("visual_share.configuration.fast_screenshot.share.max_size")
                    .defineInRange("max_size", -1, -1, 32768);
            FAST_SCREENSHOT_SHARE_PREFIX = builder
                    .translation("visual_share.configuration.fast_screenshot.share.prefix")
                    .define("prefix", "<qs_fs>");
            FAST_SCREENSHOT_SHARE_SUBFIX = builder
                    .translation("visual_share.configuration.fast_screenshot.share.subfix")
                    .define("subfix", "</qs_fs>");
            builder.pop();
        }
    }

    public static class Client {
        public final ModConfigSpec.EnumValue<DataType> FAST_SCREENSHOT_SHARE_DATA_TYPE;
        public final ModConfigSpec.IntValue FAST_SCREENSHOT_SHARE_AVIF_QUALITY;
        public final ModConfigSpec.IntValue FAST_SCREENSHOT_SHARE_AVIF_SPEED;
        public final ModConfigSpec.BooleanValue FAST_SCREENSHOT_SHARE_AVIF_LOSSLESS;
        public final ModConfigSpec.EnumValue<InterpolationAlgorithm> FAST_SCREENSHOT_SHARE_INTERPOLATION_ALGORITHM;
        public final ModConfigSpec.DoubleValue FAST_SCREENSHOT_SHARE_THUMBNAIL_IMAGE_SIZE;
        public final ModConfigSpec.DoubleValue FAST_SCREENSHOT_SHARE_FULL_IMAGE_SIZE;

        Client(ModConfigSpec.Builder builder){
            builder.push("fast_screenshot");
            builder.push("share");
            FAST_SCREENSHOT_SHARE_DATA_TYPE = builder
                    .translation("visual_share.configuration.fast_screenshot.share_data_type")
                    .defineEnum("data_type", DataType.avif);

            builder.push("share_data_param");
            FAST_SCREENSHOT_SHARE_AVIF_QUALITY = builder
                    .translation("visual_share.configuration.fast_screenshot.share_data_param.avif_quality")
                    .defineInRange("avif_quality", 75, 0, 100);
            FAST_SCREENSHOT_SHARE_AVIF_SPEED = builder
                    .translation("visual_share.configuration.fast_screenshot.share_data_param.avif_speed")
                    .defineInRange("avif_speed", 10, 0, 10);
            FAST_SCREENSHOT_SHARE_AVIF_LOSSLESS = builder
                    .translation("visual_share.configuration.fast_screenshot.share_data_param.avif_lossless")
                    .define("avif_lossless", false);
            builder.pop();

            FAST_SCREENSHOT_SHARE_INTERPOLATION_ALGORITHM = builder
                    .translation("visual_share.configuration.fast_screenshot.share_interpolation_algorithm")
                    .defineEnum("interpolation_algorithm", InterpolationAlgorithm.Nearest);
            FAST_SCREENSHOT_SHARE_THUMBNAIL_IMAGE_SIZE = builder
                    .translation("visual_share.configuration.fast_screenshot.share_thumbnail_image_size")
                    .defineInRange("thumbnail_image_size", 0.25, 0, 1);
            FAST_SCREENSHOT_SHARE_FULL_IMAGE_SIZE = builder
                    .translation("visual_share.configuration.fast_screenshot.share_full_image_size")
                    .defineInRange("full_image_size", 0.75, 0, 1);
            builder.pop();
            builder.pop();
        }
    }

    public enum OverrideMode{
        NO, MAX, MIN, ALL;
    }

    public enum DataType{
        png("png"),
        avif("avif");

        private final String type;

        DataType(String type) {
            this.type = type;
        }

        public String getType(){
            return this.type;
        }
    }

    public enum InterpolationAlgorithm{
        Nearest(Image.SCALE_REPLICATE),
        Bilinear(Image.SCALE_AREA_AVERAGING);

        private final int algorithm;

        InterpolationAlgorithm(int algorithm) {
            this.algorithm = algorithm;
        }

        public int getAlgorithm() {
            return algorithm;
        }
    }
}
