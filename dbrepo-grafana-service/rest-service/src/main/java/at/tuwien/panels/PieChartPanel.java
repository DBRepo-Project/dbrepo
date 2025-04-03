package at.tuwien.panels;

import at.tuwien.dto.PieChartConfigDto;
import at.tuwien.endpoints.DataEndpoint;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class PieChartPanel extends AbstractPanel {
    public static final String RELATIVE_PATH = "/piechart";
    public static final String VIEW_PIE_PERCENTAGE_COL = "percentage";

    private static final int HEIGHT = 8;
    private static final int WIDTH = 7;
    private String dataAPI;
    private final String colName;

    public PieChartPanel(Long dbId, Long vId, String colName, PieChartConfigDto config) {
        this.dataAPI = String.format("%s%s%s/%d/%d", dataEndpoint, DataEndpoint.API_PREFIX, RELATIVE_PATH, dbId, vId);
        this.colName = colName;

        if (config != null && config.getSize() != null) {
                dataAPI += String.format("?size=%d", config.getSize());
        }
    }

    @Override
    public String getConstructedPanel() {
        handleOverflow(HEIGHT, WIDTH);
        String panelJson = "{\n" +
                "                \"datasource\": {\n" +
                "                    \"type\": \"yesoreyeram-infinity-datasource\",\n" +
                "                    \"uid\": \"" +  DATASRC_UID + "\"" +
                "                },\n" +
                "                \"fieldConfig\": {\n" +
                "                    \"defaults\": {\n" +
                "                        \"color\": {\n" +
                "                            \"mode\": \"palette-classic\"\n" +
                "                        },\n" +
                "                        \"custom\": {\n" +
                "                            \"hideFrom\": {\n" +
                "                                \"legend\": false,\n" +
                "                                \"tooltip\": false,\n" +
                "                                \"viz\": false\n" +
                "                            }\n" +
                "                        },\n" +
                "                        \"mappings\": []\n" +
                "                    },\n" +
                "                    \"overrides\": []\n" +
                "                },\n" +
                "                \"gridPos\": {\n" +
                "                    \"h\": " + HEIGHT + ",\n" +
                "                    \"w\": " + WIDTH + ",\n" +
                "                    \"x\": " + x + ",\n" +
                "                    \"y\": " + y + "\n" +
                "                },\n" +
                "                \"options\": {\n" +
                "                    \"displayLabels\": [\n" +
                "                        \"percent\"\n" +
                "                    ],\n" +
                "                    \"legend\": {\n" +
                "                        \"calcs\": [],\n" +
                "                        \"displayMode\": \"list\",\n" +
                "                        \"placement\": \"right\",\n" +
                "                        \"showLegend\": true,\n" +
                "                        \"values\": [\n" +
                "                            \"value\"\n" +
                "                        ]\n" +
                "                    },\n" +
                "                    \"pieType\": \"pie\",\n" +
                "                    \"reduceOptions\": {\n" +
                "                        \"calcs\": [\n" +
                "                            \"lastNotNull\"\n" +
                "                        ],\n" +
                "                        \"fields\": \"\",\n" +
                "                        \"values\": false\n" +
                "                    },\n" +
                "                    \"tooltip\": {\n" +
                "                        \"mode\": \"single\",\n" +
                "                        \"sort\": \"none\"\n" +
                "                    }\n" +
                "                },\n" +
                "                \"targets\": [\n" +
                "                    {\n" +
                "                        \"columns\": [],\n" +
                "                        \"datasource\": {\n" +
                "                            \"type\": \"yesoreyeram-infinity-datasource\",\n" +
                "                            \"uid\": \"" +  DATASRC_UID + "\"" +
                "                        },\n" +
                "                        \"filters\": [],\n" +
                "                        \"format\": \"table\",\n" +
                "                        \"global_query_id\": \"\",\n" +
                "                        \"refId\": \"A\",\n" +
                "                        \"root_selector\": \"\",\n" +
                "                        \"source\": \"url\",\n" +
                "                        \"type\": \"json\",\n" +
                "                        \"url\": \"" + this.dataAPI + "\",\n" +
                "                        \"url_options\": {\n" +
                "                            \"data\": \"\",\n" +
                "                            \"method\": \"GET\"\n" +
                "                        }\n" +
                "                    }\n" +
                "                ],\n" +
                "                \"title\": \"Distribution of Most Frequent " + this.colName + "\",\n" +
                "                \"type\": \"piechart\"\n" +
                "            }";

        updateCoords(HEIGHT, WIDTH);
        return panelJson;
    }
}
