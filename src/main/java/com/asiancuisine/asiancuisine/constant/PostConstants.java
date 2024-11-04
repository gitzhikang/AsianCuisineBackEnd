package com.asiancuisine.asiancuisine.constant;

public class PostConstants {
    public static final String MAPPING_TEMPLATE =
            "{\n" +
            "  \"mappings\":{\n" +
            "    \"properties\":{\n" +
            "      \"id\":{\n" +
            "        \"type\":\"keyword\"\n" +
            "      },\n" +
            "      \"user_id\":{\n" +
            "        \"type\":\"keyword\"\n" +
            "      },\n" +
            "      \"title\":{\n" +
            "        \"type\":\"text\"\n" +
            "      },\n" +
            "      \"text\":{\n" +
            "        \"type\":\"text\"\n" +
            "      },\n" +
            "      \"post_comment\":{\n" +
            "        \"type\":\"object\",\n" +
            "        \"properties\":{\n" +
            "          \"content\":{\n" +
            "            \"type\": \"text\"\n" +
            "          },\n" +
            "          \"create_time\":{\n" +
            "            \"type\": \"date\",\n" +
            "            \"format\": \"yyyy-MM-dd HH:mm:ss\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      \"liked_count\":{\n" +
            "        \"type\":\"long\"\n" +
            "      },\n" +
            "      \"comment_count\":{\n" +
            "        \"type\":\"long\"\n" +
            "      },\n" +
            "      \"create_time\":{\n" +
            "        \"type\": \"date\",\n" +
            "        \"format\": \"yyyy-MM-dd HH:mm:ss\"\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}";
}
