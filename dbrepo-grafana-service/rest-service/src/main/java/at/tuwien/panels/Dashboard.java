package at.tuwien.panels;

import java.util.List;

public class Dashboard {

    public String getDashboard(List<String> panels, Long dbId, int refreshrate) {

        return "{\n" +
                "    \"dashboard\": {\n" +
                "        \"id\": null,\n" +
                "        \"uid\": \"" + dbId + "\",\n" +
                "        \"title\": \"automated dashboard_" + dbId + "\",\n" +
                "        \"tags\": [\n" +
                "            \"templated\"\n" +
                "        ],\n" +
                "        \"timezone\": \"browser\",\n" +
                "        \"schemaVersion\": 16,\n" +
                "        \"refresh\": \"" + (refreshrate == 0 ? "" : refreshrate + "s") + "\",\n" +
                "        \"panels\": [" +
                String.join(", ", panels) +
                "                    ]" +
                "        \n" +
                "    },\n" +
                "    \"message\": \"automated creation of dashboard\",\n" +
                "    \"overwrite\": false\n" +
                "}";
    }
}
