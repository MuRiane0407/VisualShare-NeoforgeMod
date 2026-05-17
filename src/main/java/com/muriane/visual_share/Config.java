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
        public final ModConfigSpec.EnumValue<OverrideMode> SCREENSHOT_SHARE_OVERRIDE_CLIENT_PARAM;
        public final ModConfigSpec.EnumValue<DataType> SCREENSHOT_SHARE_DATA_TYPE;
        public final ModConfigSpec.IntValue SCREENSHOT_SHARE_AVIF_QUALITY;
        public final ModConfigSpec.IntValue SCREENSHOT_SHARE_AVIF_SPEED;
        public final ModConfigSpec.BooleanValue SCREENSHOT_SHARE_AVIF_LOSSLESS;
        public final ModConfigSpec.EnumValue<InterpolationAlgorithm> SCREENSHOT_SHARE_INTERPOLATION_ALGORITHM;
        public final ModConfigSpec.IntValue SCREENSHOT_SHARE_MAX_SIZE;
        public final ModConfigSpec.IntValue SCREENSHOT_SHARE_COOLDOWN;
        public final ModConfigSpec.ConfigValue<String> SCREENSHOT_SHARE_IMAGE_PREFIX;
        public final ModConfigSpec.ConfigValue<String> SCREENSHOT_SHARE_IMAGE_SUBFIX;

        Server(ModConfigSpec.Builder builder){
            builder.push("screenshot");
            builder.push("share");
            SCREENSHOT_SHARE_OVERRIDE_CLIENT_PARAM = builder
                    .translation("visual_share.configuration.screenshot.share.override_client_param")
                    .defineEnum("override_client_param", OverrideMode.MAX);

            builder.push("override_share_param");
            SCREENSHOT_SHARE_DATA_TYPE = builder
                    .translation("visual_share.configuration.screenshot.share_data_type")
                    .defineEnum("data_type", DataType.AVIF);

            builder.push("share_param");
            SCREENSHOT_SHARE_AVIF_QUALITY = builder
                    .translation("visual_share.configuration.screenshot.share_param.avif_quality")
                    .defineInRange("avif_quality", 75, 0, 100);
            SCREENSHOT_SHARE_AVIF_SPEED = builder
                    .translation("visual_share.configuration.screenshot.share_param.avif_speed")
                    .defineInRange("avif_speed", 10, 0, 10);
            SCREENSHOT_SHARE_AVIF_LOSSLESS = builder
                    .translation("visual_share.configuration.screenshot.share_param.avif_lossless")
                    .define("avif_lossless", false);
            builder.pop();

            SCREENSHOT_SHARE_INTERPOLATION_ALGORITHM = builder
                    .translation("visual_share.configuration.screenshot.share.interpolation_algorithm")
                    .defineEnum("interpolation_algorithm", InterpolationAlgorithm.Nearest);
            builder.pop();

            SCREENSHOT_SHARE_MAX_SIZE = builder
                    .translation("visual_share.configuration.screenshot.share.max_size")
                    .defineInRange("max_size", 1920, -1, 32768);
            SCREENSHOT_SHARE_COOLDOWN = builder
                    .translation("visual_share.configuration.screenshot.share.cooldown")
                    .defineInRange("cooldown", 3, 0, 300);
            SCREENSHOT_SHARE_IMAGE_PREFIX = builder
                    .translation("visual_share.configuration.screenshot.share.image_prefix")
                    .define("prefix", "<qs_fs>");
            SCREENSHOT_SHARE_IMAGE_SUBFIX = builder
                    .translation("visual_share.configuration.screenshot.share.image_subfix")
                    .define("subfix", "</qs_fs>");
            builder.pop();
        }
    }

    public static class Client {
        public final ModConfigSpec.EnumValue<DataType> SCREENSHOT_SHARE_DATA_TYPE;
        public final ModConfigSpec.IntValue SCREENSHOT_SHARE_AVIF_QUALITY;
        public final ModConfigSpec.IntValue SCREENSHOT_SHARE_AVIF_SPEED;
        public final ModConfigSpec.BooleanValue SCREENSHOT_SHARE_AVIF_LOSSLESS;
        public final ModConfigSpec.EnumValue<InterpolationAlgorithm> SCREENSHOT_SHARE_INTERPOLATION_ALGORITHM;
        public final ModConfigSpec.DoubleValue SCREENSHOT_SHARE_THUMBNAIL_IMAGE_SIZE;
        public final ModConfigSpec.DoubleValue SCREENSHOT_SHARE_FULL_IMAGE_SIZE;

        Client(ModConfigSpec.Builder builder){
            builder.push("screenshot");
            builder.push("share");
            SCREENSHOT_SHARE_DATA_TYPE = builder
                    .translation("visual_share.configuration.screenshot.share_data_type")
                    .defineEnum("data_type", DataType.AVIF);

            builder.push("share_param");
            SCREENSHOT_SHARE_AVIF_QUALITY = builder
                    .translation("visual_share.configuration.screenshot.share_param.avif_quality")
                    .defineInRange("avif_quality", 75, 0, 100);
            SCREENSHOT_SHARE_AVIF_SPEED = builder
                    .translation("visual_share.configuration.screenshot.share_param.avif_speed")
                    .defineInRange("avif_speed", 10, 0, 10);
            SCREENSHOT_SHARE_AVIF_LOSSLESS = builder
                    .translation("visual_share.configuration.screenshot.share_param.avif_lossless")
                    .define("avif_lossless", false);
            builder.pop();

            SCREENSHOT_SHARE_INTERPOLATION_ALGORITHM = builder
                    .translation("visual_share.configuration.screenshot.share.interpolation_algorithm")
                    .defineEnum("interpolation_algorithm", InterpolationAlgorithm.Nearest);
            SCREENSHOT_SHARE_THUMBNAIL_IMAGE_SIZE = builder
                    .translation("visual_share.configuration.screenshot.share.thumbnail_image_size")
                    .defineInRange("thumbnail_image_size", 0.25, 0, 1);
            SCREENSHOT_SHARE_FULL_IMAGE_SIZE = builder
                    .translation("visual_share.configuration.screenshot.share.full_image_size")
                    .defineInRange("full_image_size", 0.75, 0, 1);
            builder.pop();
            builder.pop();
        }
    }

    public enum OverrideMode{
        NO, MAX, MIN, ALWAYS;
    }

    public enum DataType{
        PNG("png"),
        AVIF("avif");

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
