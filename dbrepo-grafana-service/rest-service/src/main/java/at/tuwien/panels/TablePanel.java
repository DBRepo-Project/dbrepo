package at.tuwien.panels;

import at.tuwien.endpoints.DataEndpoint;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class TablePanel extends AbstractPanel {
    public static final String RELATIVE_PATH = "/tablepanel";
    private static final int HEIGHT = 8;
    private static final int WIDTH = 16;
    private final String name;

    private String dataAPI;

    public TablePanel(Long dbId, Long tId, String name, Long size) {
        this.name = name;
        this.dataAPI = String.format("%s%s%s/%d/%d", dataEndpoint, DataEndpoint.API_PREFIX, RELATIVE_PATH, dbId, tId);
        if (size != null) {
            dataAPI += String.format("?size=%d", size);
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
                "               \"fieldConfig\": {\n" +
                "                   \"defaults\": {\n" +
                "                       \"custom\": {\n" +
                "                            \"filterable\": true\n" +
                "                       }\n" +
                "                   }\n" +
                "               }," +
                "                \"gridPos\": {\n" +
                "                    \"h\": " + HEIGHT + ",\n" +
                "                    \"w\": " + WIDTH + ",\n" +
                "                    \"x\": " + x + ",\n" +
                "                    \"y\": " + y + "\n" +
                "                },\n" +
                "                \"id\": null,\n" +
                "                \"targets\": [\n" +
                "                    {\n" +
                "                        \"columns\": [],\n" +
                "                        \"datasource\": {\n" +
                "                            \"type\": \"yesoreyeram-infinity-datasource\",\n" +
                "                        \"uid\": \"" +  DATASRC_UID + "\"" +
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
                "                \"title\": \"" + name + "\",\n" +
                "                \"type\": \"table\"\n" +
                "            }";

        updateCoords(HEIGHT, WIDTH);
        return panelJson;
    }
}
