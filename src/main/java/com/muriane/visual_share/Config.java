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
        public final ModConfigSpec.BooleanValue ENABLE_ITEM_SHOW;
        public final ModConfigSpec.ConfigValue<String> ITEM_SHOW_PREFIX;
        public final ModConfigSpec.ConfigValue<String> ITEM_SHOW_SUBFIX;
        public final ModConfigSpec.ConfigValue<String> ITEM_SHOW_CUSTOM_EMPTY_NAME;
        public final ModConfigSpec.ConfigValue<String> ITEM_SHOW_CUSTOM_EMPTY_DESCRIPTION;
        public final ModConfigSpec.BooleanValue ENABLE_INVENTORY_SHOW;
        public final ModConfigSpec.ConfigValue<String> INVENTORY_SHOW_MARKER;
        public final ModConfigSpec.BooleanValue ENABLE_ENDER_CHEST_SHOW;
        public final ModConfigSpec.ConfigValue<String> ENDER_CHEST_SHOW_MARKER;

        public final ModConfigSpec.BooleanValue ENABLE_SCREENSHOT_SHARE;
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
            builder.push("misc");

            builder.push("item_show");
            ENABLE_ITEM_SHOW = builder
                    .translation("visual_share.configuration.misc.item_show.enable")
                    .define("enable", true);
            ITEM_SHOW_PREFIX = builder
                    .translation("visual_share.configuration.misc.item_show.prefix")
                    .define("prefix", "[i");
            ITEM_SHOW_SUBFIX = builder
                    .translation("visual_share.configuration.misc.item_show.subfix")
                    .define("subfix", "]");
            ITEM_SHOW_CUSTOM_EMPTY_NAME = builder
                    .translation("visual_share.configuration.misc.item_show.custom_empty_name")
                    .define("custom_empty_name", "");
            ITEM_SHOW_CUSTOM_EMPTY_DESCRIPTION = builder
                    .translation("visual_share.configuration.misc.item_show.custom_empty_description")
                    .define("custom_empty_description", "");
            builder.pop();

            builder.push("inventory_show");
            ENABLE_INVENTORY_SHOW = builder
                    .translation("visual_share.configuration.misc.inventory_show.enable")
                    .define("enable", true);
            INVENTORY_SHOW_MARKER = builder
                    .translation("visual_share.configuration.misc.inventory_show.marker")
                    .define("marker", "[inv]");
            builder.pop();

            builder.push("ender_chest_show");
            ENABLE_ENDER_CHEST_SHOW = builder
                    .translation("visual_share.configuration.misc.ender_chest_show.enable")
                    .define("enable", true);
            ENDER_CHEST_SHOW_MARKER = builder
                    .translation("visual_share.configuration.misc.ender_chest_show.marker")
                    .define("marker", "[ec]");
            builder.pop();

            builder.pop();

            builder.push("screenshot");
            builder.push("share");
            ENABLE_SCREENSHOT_SHARE = builder
                    .translation("visual_share.configuration.screenshot.share.enable")
                    .define("enable", true);
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
                    .define("image_prefix", "<qs_fs>");
            SCREENSHOT_SHARE_IMAGE_SUBFIX = builder
                    .translation("visual_share.configuration.screenshot.share.image_subfix")
                    .define("image_subfix", "</qs_fs>");
            builder.pop();
            builder.pop();
        }
    }

    public static class Client {
        public final ModConfigSpec.BooleanValue ITEM_SHOW_DIRECTLY_SEND;

        public final ModConfigSpec.EnumValue<DataType> SCREENSHOT_SHARE_DATA_TYPE;
        public final ModConfigSpec.IntValue SCREENSHOT_SHARE_AVIF_QUALITY;
        public final ModConfigSpec.IntValue SCREENSHOT_SHARE_AVIF_SPEED;
        public final ModConfigSpec.BooleanValue SCREENSHOT_SHARE_AVIF_LOSSLESS;
        public final ModConfigSpec.EnumValue<InterpolationAlgorithm> SCREENSHOT_SHARE_INTERPOLATION_ALGORITHM;
        public final ModConfigSpec.DoubleValue SCREENSHOT_SHARE_THUMBNAIL_IMAGE_SIZE;
        public final ModConfigSpec.DoubleValue SCREENSHOT_SHARE_FULL_IMAGE_SIZE;
        public final ModConfigSpec.IntValue SCREENSHOT_RERENDER_FREQUENCY;
        public final ModConfigSpec.BooleanValue SCREENSHOT_AUTO_CLOSE_SIDEBAR;

        public final ModConfigSpec.IntValue DRAWING_BOARD_WIDTH;
        public final ModConfigSpec.IntValue DRAWING_BOARD_HEIGHT;
        public final ModConfigSpec.IntValue DRAWING_BOARD_BACKGROUND_COLOR;

        Client(ModConfigSpec.Builder builder){
            builder.push("misc");
            ITEM_SHOW_DIRECTLY_SEND = builder
                    .translation("visual_share.configuration.misc.directly_send")
                    .define("directly_send", true);
            builder.pop();

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

            SCREENSHOT_RERENDER_FREQUENCY = builder
                    .translation("visual_share.configuration.screenshot.rerender_frequency")
                    .defineInRange("rerender_frequency", 15, 1, 120);
            SCREENSHOT_AUTO_CLOSE_SIDEBAR = builder
                    .translation("visual_share.configuration.screenshot.auto_close_sidebar")
                    .define("auto_close_sidebar", false);
            builder.pop();

            builder.push("drawing_board");
            DRAWING_BOARD_WIDTH = builder
                    .translation("visual_share.configuration.drawing_board.width")
                    .defineInRange("width", 256, 32, 2048);
            DRAWING_BOARD_HEIGHT = builder
                    .translation("visual_share.configuration.drawing_board.height")
                    .defineInRange("height", 256, 32, 2048);
            DRAWING_BOARD_BACKGROUND_COLOR = builder
                    .translation("visual_share.configuration.drawing_board.background_color")
                    .defineInRange("background_color", 16777215, 0, 16777215);
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
