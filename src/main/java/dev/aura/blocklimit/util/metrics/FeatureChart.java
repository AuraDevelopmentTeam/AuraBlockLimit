package dev.aura.blocklimit.util.metrics;

import dev.aura.blocklimit.AuraBlockLimit;
import dev.aura.blocklimit.config.Config;
import java.util.HashMap;
import java.util.LinkedHashMap;
import org.bstats.sponge.Metrics2.SimpleBarChart;

public class FeatureChart extends SimpleBarChart {
  public static HashMap<String, Integer> getValues() {
    HashMap<String, Integer> sortedMap = new LinkedHashMap<>();

    final Config config = AuraBlockLimit.getConfig();
    final Config.Storage storageConfig = config.getStorage();

    sortedMap.put("MySQL", storageConfig.isMySQL() ? 1 : 0);
    sortedMap.put("H2", storageConfig.isH2() ? 1 : 0);

    sortedMap.put("Servers", 1);

    return sortedMap;
  }

  public FeatureChart(String chartId) {
    super(chartId, FeatureChart::getValues);
  }
}
