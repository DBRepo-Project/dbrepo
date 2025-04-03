package at.tuwien.panels;

import at.tuwien.endpoints.DataEndpoint;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class StatsPanel extends AbstractPanel {
    public static final String RELATIVE_PATH = "/stats";
    public static final String HEADER_COL = "column";
    public static final String HEADER_MIN = "min";
    public static final String HEADER_MAX = "max";
    public static final String HEADER_STDDEV = "stddev";
    public static final String HEADER_AVG = "median";

    private static final int HEIGHT = 8;
    private static final int WIDTH = 9;
    private final String dataAPI;
    private final String name;

    public StatsPanel(Long dbId, Long tId, String name) {
        this.dataAPI = String.format("%s%s%s/%d/%d", dataEndpoint, DataEndpoint.API_PREFIX, RELATIVE_PATH, dbId, tId);
        this.name = name;
    }

    @Override
    public String getConstructedPanel() {
        handleOverflow(HEIGHT, WIDTH);
        String panelJson = " {\n" +
                "      \"datasource\": {\n" +
                "        \"type\": \"yesoreyeram-infinity-datasource\",\n" +
                "         \"uid\": \"" +  DATASRC_UID + "\"" +
                "      },\n" +
                "      \"fieldConfig\": {\n" +
                "        \"defaults\": {\n" +
                "          \"custom\": {\n" +
                "            \"align\": \"auto\",\n" +
                "            \"filterable\": \"true\",\n" +
                "            \"cellOptions\": {\n" +
                "              \"type\": \"auto\"\n" +
                "            },\n" +
                "            \"inspect\": false\n" +
                "          },\n" +
                "          \"mappings\": [],\n" +
                "          \"thresholds\": {\n" +
                "            \"mode\": \"absolute\",\n" +
                "            \"steps\": [\n" +
                "              {\n" +
                "                \"color\": \"green\",\n" +
                "                \"value\": null\n" +
                "              },\n" +
                "              {\n" +
                "                \"color\": \"red\",\n" +
                "                \"value\": 80\n" +
                "              }\n" +
                "            ]\n" +
                "          }\n" +
                "        },\n" +
                "        \"overrides\": [\n" +
                "          {\n" +
                "            \"matcher\": {\n" +
                "              \"id\": \"byName\",\n" +
                "              \"options\": \"" + HEADER_COL + "\"\n" +
                "            },\n" +
                "            \"properties\": [\n" +
                "              {\n" +
                "                \"id\": \"custom.align\",\n" +
                "                \"value\": \"center\"\n" +
                "              }\n" +
                "            ]\n" +
                "          },\n" +
                "          {\n" +
                "            \"matcher\": {\n" +
                "              \"id\": \"byName\",\n" +
                "              \"options\": \"" + HEADER_MIN + "\"\n" +
                "            },\n" +
                "            \"properties\": [\n" +
                "              {\n" +
                "                \"id\": \"custom.width\",\n" +
                "                \"value\": 115\n" +
                "              }\n" +
                "            ]\n" +
                "          },\n" +
                "          {\n" +
                "            \"matcher\": {\n" +
                "              \"id\": \"byName\",\n" +
                "              \"options\": \"" + HEADER_MAX + "\"\n" +
                "            },\n" +
                "            \"properties\": [\n" +
                "              {\n" +
                "                \"id\": \"custom.width\",\n" +
                "                \"value\": 115\n" +
                "              }\n" +
                "            ]\n" +
                "          },\n" +
                "          {\n" +
                "            \"matcher\": {\n" +
                "              \"id\": \"byName\",\n" +
                "              \"options\": \"" + HEADER_AVG + "\"\n" +
                "            },\n" +
                "            \"properties\": [\n" +
                "              {\n" +
                "                \"id\": \"custom.width\",\n" +
                "                \"value\": 115\n" +
                "              }\n" +
                "            ]\n" +
                "          },\n" +
                "          {\n" +
                "            \"matcher\": {\n" +
                "              \"id\": \"byName\",\n" +
                "              \"options\": \"" + HEADER_STDDEV + "\"\n" +
                "            },\n" +
                "            \"properties\": [\n" +
                "              {\n" +
                "                \"id\": \"custom.width\",\n" +
                "                \"value\": 115\n" +
                "              }\n" +
                "            ]\n" +
                "          }\n" +
                "        ]\n" +
                "      },\n" +
                "                \"gridPos\": {\n" +
                "                    \"h\": " + HEIGHT + ",\n" +
                "                    \"w\": " + WIDTH + ",\n" +
                "                    \"x\": " + x + ",\n" +
                "                    \"y\": " + y + "\n" +
                "                },\n" +
                "      \"options\": {\n" +
                "        \"cellHeight\": \"sm\",\n" +
                "        \"footer\": {\n" +
                "          \"countRows\": false,\n" +
                "          \"fields\": \"\",\n" +
                "          \"reducer\": [\n" +
                "            \"sum\"\n" +
                "          ],\n" +
                "          \"show\": false\n" +
                "        },\n" +
                "        \"showHeader\": true\n" +
                "      },\n" +
                "      \"targets\": [\n" +
                "        {\n" +
                "          \"columns\": [],\n" +
                "          \"datasource\": {\n" +
                "            \"type\": \"yesoreyeram-infinity-datasource\",\n" +
                "            \"uid\": \"" +  DATASRC_UID + "\"" +
                "          },\n" +
                "          \"filters\": [],\n" +
                "          \"format\": \"table\",\n" +
                "          \"global_query_id\": \"\",\n" +
                "          \"refId\": \"A\",\n" +
                "          \"root_selector\": \"\",\n" +
                "          \"source\": \"url\",\n" +
                "          \"type\": \"json\",\n" +
                "          \"url\": \"" + this.dataAPI + "\",\n" +
                "          \"url_options\": {\n" +
                "            \"data\": \"\",\n" +
                "            \"method\": \"GET\"\n" +
                "          }\n" +
                "        }\n" +
                "      ],\n" +
                "      \"title\": \"Stats for " + name + "\",\n" +
                "      \"transformations\": [\n" +
                "        {\n" +
                "          \"id\": \"organize\",\n" +
                "          \"options\": {\n" +
                "            \"excludeByName\": {},\n" +
                "            \"includeByName\": {},\n" +
                "            \"indexByName\": {\n" +
                "              \"" + HEADER_AVG + "\": 3,\n" +
                "              \"" + HEADER_COL + "\": 0,\n" +
                "              \"" + HEADER_STDDEV + "\": 4,\n" +
                "              \"" + HEADER_MAX + "\": 2,\n" +
                "              \"" + HEADER_MIN + "\": 1\n" +
                "            }\n" +
                "          }\n" +
                "        }\n" +
                "      ],\n" +
                "      \"type\": \"table\"\n" +
                "    }";

        updateCoords(HEIGHT, WIDTH);
        return panelJson;
    }
}
