package at.tuwien.panels;

import at.tuwien.endpoints.DataEndpoint;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class CntAllPanel extends AbstractPanel {
    public static final String RELATIVE_PATH = "/cntAll";
    private static final int HEIGHT = 8;
    private static final int WIDTH = 5;
    private final String dataAPI;

    public CntAllPanel(Long dbId, Long vId) {
        this.dataAPI = String.format("%s%s%s/%d/%d", dataEndpoint, DataEndpoint.API_PREFIX, RELATIVE_PATH, dbId, vId);
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
                "                            \"fixedColor\": \"#FFFFFF\",\n" +
                "                            \"mode\": \"fixed\"\n" +
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
                "                \"title\": \"Total elements\",\n" +
                "                \"type\": \"stat\"\n" +
                "            }";

        updateCoords(HEIGHT, WIDTH);
        return panelJson;
    }
}
