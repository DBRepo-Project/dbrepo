package at.tuwien.endpoints;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractEndpoint {

    public List<Map<String, Object>> transform(Dataset<Row> dataset) {
        return dataset.collectAsList()
                .stream()
                .map(row -> {
                    final Map<String, Object> map = new LinkedHashMap<>();
                    for (int i = 0; i < dataset.columns().length; i++) {
                        if (row.get(i) == null) {
                            map.put(dataset.columns()[i], null);
                            continue;
                        }
                        try {
                            map.put(dataset.columns()[i], Integer.parseInt(String.valueOf(row.get(i))));
                            map.put(dataset.columns()[i], Double.parseDouble(String.valueOf(row.get(i))));
                        } catch (NumberFormatException e) {
                            /* ignore */
                        }
                        map.put(dataset.columns()[i], String.valueOf(row.get(i)));
                    }
                    return map;
                })
                .toList();
    }
}
