package at.tuwien.panels;

import at.tuwien.dto.HistogramConfigDto;
import at.tuwien.endpoints.DataEndpoint;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class HistogramPanel extends AbstractPanel {
    public static final String RELATIVE_PATH = "/histogram";
    private static final int HEIGHT = 8;
    private static final int WIDTH = 5;
    private String dataAPI;
    private final String colName;
    private Integer min;
    private Integer max;

    public HistogramPanel(Long dbId, Long vId, String colName, HistogramConfigDto configDto) {
        this.dataAPI = String.format("%s%s%s/%d/%d", dataEndpoint, DataEndpoint.API_PREFIX, RELATIVE_PATH, dbId, vId);
        this.colName = colName;

        if (configDto != null) {
            if (configDto.getMin() != null) {
                this.min = configDto.getMin();
            }

            if (configDto.getMax() != null) {
                this.max = configDto.getMax();
            }

            if (configDto.getSize() != null) {
                dataAPI += String.format("?size=%d", configDto.getSize());
            }
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
                "                            \"fillOpacity\": 80,\n" +
                "                            \"gradientMode\": \"none\",\n" +
                "                            \"hideFrom\": {\n" +
                "                                \"legend\": false,\n" +
                "                                \"tooltip\": true,\n" +
                "                                \"viz\": false\n" +
                "                            },\n" +
                "                            \"lineWidth\": 1\n" +
                "                        },\n" +
                "                        \"mappings\": [],\n" +
                "                        \"max\": " + ((max == null) ? "null" : max) + ",\n" +
                "                        \"min\": " + ((min == null) ? "null" : min) + "\n" +
                "                    },\n" +
                "                    \"overrides\": []\n" +
                "                },\n" +
                "                \"gridPos\": {\n" +
                "                    \"h\": " + HEIGHT + ",\n" +
                "                    \"w\": " + WIDTH + ",\n" +
                "                    \"x\": " + x + ",\n" +
                "                    \"y\": " + y + "\n" +
                "                },\n" +
                "                \"id\": null,\n" +
                "                \"description\": \"" + "Each bar represents a bucket, and the bar height represents " +
                "the frequency of the values from the column " + this.colName + " that fell into that bucket's interval.\",\n" +
                "                \"options\": {\n" +
                "                    \"legend\": {\n" +
                "                        \"calcs\": [],\n" +
                "                        \"displayMode\": \"list\",\n" +
                "                        \"placement\": \"bottom\",\n" +
                "                        \"showLegend\": false\n" +
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
                "                        \"root_selector\": \"values\",\n" +
                "                        \"source\": \"url\",\n" +
                "                        \"type\": \"json\",\n" +
                "                        \"url\": \"" + this.dataAPI + "\",\n" +
                "                        \"url_options\": {\n" +
                "                            \"data\": \"\",\n" +
                "                            \"method\": \"GET\"\n" +
                "                        }\n" +
                "                    }\n" +
                "                ],\n" +
                "                \"title\": \"" + "Distribution of " + this.colName + " \",\n" +
                "                \"type\": \"histogram\"\n" +
                "            }";

        updateCoords(HEIGHT, WIDTH);
        return panelJson;
    }
}
