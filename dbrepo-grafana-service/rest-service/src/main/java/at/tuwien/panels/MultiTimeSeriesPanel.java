package at.tuwien.panels;

import at.tuwien.endpoints.DataEndpoint;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class MultiTimeSeriesPanel extends AbstractPanel {
    public static final String RELATIVE_PATH = "/multitimeseries";
    public static final String VIEW_MULTI_TIMECOL = "timecol";
    public static final String VIEW_MULTI_SELECTOR_TIME = "time";
    public static final String VIEW_MULTI_SELECTOR_VALUE = "value";
    public static final String VIEW_MULTI_SELECTOR_NAME = "name";


    private static final int HEIGHT = 8;
    private static final int WIDTH = 5;
    private String dataAPI;

    public MultiTimeSeriesPanel(Long dbId, Long vId) {
        this.dataAPI = String.format("%s%s%s/%d/%d", dataEndpoint, DataEndpoint.API_PREFIX, RELATIVE_PATH, dbId, vId);
        dataAPI += String.format("?size=%d", 100);
    }

    @Override
    public String getConstructedPanel() {
        handleOverflow(HEIGHT, WIDTH);
        String panelJson = " {\n" +
                "\"datasource\": {\n" +
                "                    \"uid\": \"" +  DATASRC_UID + "\",\n" +
                "                    \"type\": \"yesoreyeram-infinity-datasource\"\n" +
                "                },\n" +
                "                \"type\": \"timeseries\",\n" +
                "                \"title\": \"Time Series\",\n" +
                "                \"gridPos\": {\n" +
                "                    \"h\": " + HEIGHT + ",\n" +
                "                    \"w\": " + WIDTH + ",\n" +
                "                    \"x\": " + x + ",\n" +
                "                    \"y\": " + y + "\n" +
                "                },\n" +
                "\"options\": {\n" +
                "                            \"legend\": {\n" +
                "                                \"calcs\": [],\n" +
                "                                \"displayMode\": \"list\",\n" +
                "                                \"placement\": \"bottom\",\n" +
                "                                \"showLegend\": true\n" +
                "                            },\n" +
                "                            \"tooltip\": {\n" +
                "                                \"mode\": \"single\",\n" +
                "                                \"sort\": \"none\"\n" +
                "                            }\n" +
                "                        },\n" +
                "                \"targets\": [\n" +
                "                    {\n" +
                "                        \"datasource\": {\n" +
                "                            \"type\": \"yesoreyeram-infinity-datasource\",\n" +
                "                            \"uid\": \"" +  DATASRC_UID + "\"" +
                "                        },\n" +
                "                        \"refId\": \"A\",\n" +
                "                        \"type\": \"json\",\n" +
                "                        \"source\": \"url\",\n" +
                "                        \"format\": \"timeseries\",\n" +
                "                        \"url\": \"" + this.dataAPI + "\",\n" +
                "                        \"url_options\": {\n" +
                "                            \"method\": \"GET\",\n" +
                "                            \"data\": \"\"\n" +
                "                        },\n" +
                "                        \"root_selector\": \"time_series\",\n" +
                "                        \"columns\": [\n" +
                "                            {\n" +
                "                                \"selector\": \"" + VIEW_MULTI_SELECTOR_TIME + "\",\n" +
                "                                \"type\": \"timestamp\"\n" +
                "                            },\n" +
                "                            {\n" +
                "                                \"selector\": \"" + VIEW_MULTI_SELECTOR_VALUE + "\",\n" +
                "                                \"type\": \"number\"\n" +
                "                            },\n" +
                "                            {\n" +
                "                                \"selector\": \"" + VIEW_MULTI_SELECTOR_NAME + "\",\n" +
                "                                \"type\": \"string\"\n" +
                "                            }\n" +
                "                        ],\n" +
                "                        \"filters\": [],\n" +
                "                        \"global_query_id\": \"\"\n" +
                "                    }\n" +
                "                ]}";

        updateCoords(HEIGHT, WIDTH);
        return panelJson;
    }
}
