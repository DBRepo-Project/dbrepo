package at.tuwien.panels;

import at.tuwien.exception.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;

import java.util.List;

@Log4j2
public class RowPanel extends AbstractPanel {
    private static final int HEIGHT = 1;
    private static final int WIDTH = 24;
    private final String name;
    private final List<String> tablePanels;
    private final ObjectMapper mapper;

    public RowPanel(String name, List<String> tablePanels) {
        this.name = name;
        this.tablePanels = tablePanels;
        this.mapper = new ObjectMapper();
    }

    @Override
    public String getConstructedPanel() {

        int rowY = -1;
        try{
            JsonNode rootNode = mapper.readTree(tablePanels.get(0));
            rowY = rootNode.path("gridPos").path("y").asInt() - 1;
        } catch (Exception e) {
            log.debug("failed to read json of table panel");
            throw new JsonProcessingException("Failed to parse table panel");
        }

        return "{\n" +
                "                \"collapsed\": true,\n" +
                "                \"gridPos\": {\n" +
                "                    \"h\": " + HEIGHT + ",\n" +
                "                    \"w\": " + WIDTH + ",\n" +
                "                    \"x\": " + 0 + ",\n" +
                "                    \"y\": " + rowY + "\n" +
                "                },\n" +
                "                \"id\": null,\n" +
                "                \"panels\": [" +
                String.join(", ", tablePanels) +
                "],\n" +
                "                \"title\": \"" + name + "\",\n" +
                "                \"type\": \"row\"\n" +
                "            }";
    }
}
