package at.tuwien.panels;

import at.tuwien.endpoints.DataEndpoint;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class TimeSeriesPanel extends AbstractPanel {
    public static final String RELATIVE_PATH = "/timeseries";
    public static final String TIME_VAL_COL = "value";
    public static final String TIME_YEAR_COL = "year";
    public static final String TIME_MONTH_COL = "month";
    public static final String TIME_DAY_COL = "day";
    public static final String TIME_HOUR_COL = "hour";
    public static final String TIME_MIN_COL = "min";
    public static final String TIME_SECOND_COL = "sec";
    private final String value;

    private static final int HEIGHT = 8;
    private static final int WIDTH = 5;
    private String dataAPI;

    public TimeSeriesPanel(Long dbId, Long vId, String value, Long size) {
        this.dataAPI = String.format("%s%s%s/%d/%d", dataEndpoint, DataEndpoint.API_PREFIX, RELATIVE_PATH, dbId, vId);
        this.value = value;
        if (size != null) {
            dataAPI += String.format("?size=%d", size);
        }
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
                "                        \"format\": \"table\",\n" +
                "                        \"url\": \"" + this.dataAPI + "\",\n" +
                "                        \"url_options\": {\n" +
                "                            \"method\": \"GET\",\n" +
                "                            \"data\": \"\"\n" +
                "                        },\n" +
                "                        \"root_selector\": \"time_series\",\n" +
                "                        \"columns\": [\n" +
                "                            {\n" +
                "                                \"selector\": \"time\",\n" +
                "                                \"type\": \"timestamp\"\n" +
                "                            },\n" +
                "                            {\n" +
                "                                \"text\": \"" + value + "\",\n" +
                "                                \"selector\": \"value\",\n" +
                "                                \"type\": \"number\"\n" +
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
