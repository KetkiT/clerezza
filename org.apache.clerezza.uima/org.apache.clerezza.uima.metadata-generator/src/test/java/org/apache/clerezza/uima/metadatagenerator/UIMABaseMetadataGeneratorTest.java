package org.apache.clerezza.uima.metadatagenerator;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.utils.GraphNode;
import org.apache.clerezza.uima.utils.ExternalServicesFacade;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.fail;

/**
 * Testcase for {@link UIMABaseMetadataGenerator}
 */
public class UIMABaseMetadataGeneratorTest {
  private static final String TEXT_TO_ANALYZE = "Italy, the defending champions and four-time World Cup winners, suffer a shock World Cup defeat to Slovakia, who win a remarkable game 3-2 to book their place in the last 16";

  @Test
  public void testConstructor() {
    try {
      new UIMABaseMetadataGenerator();
    }
    catch (Exception e) {
      fail(e.getLocalizedMessage());
    }
  }

  @Test
  public void testGenerateMethodWithUnsupportedMediaType() {
    try {
      UIMABaseMetadataGenerator baseMetadataGenerator = new UIMABaseMetadataGenerator();
      String textToAnalyze = TEXT_TO_ANALYZE;
      MGraph mGraph = new SimpleMGraph();
      GraphNode node = new GraphNode(new UriRef("test"), mGraph);
      MediaType wrongMediaType = MediaType.valueOf("multipart/form-data; boundary=AaB03x");
      baseMetadataGenerator.generate(node, textToAnalyze.getBytes(), wrongMediaType);
    } catch (Exception e) {
      fail(e.getLocalizedMessage());
    }

  }

  @Test
  public void testGenerateMethodWithsupportedMediaType() {
    try {

      ExternalServicesFacade externalServicesFacade = new ExternalServicesFacade();
      Map<String, Object> parameterSettings = new HashMap<String, Object>();
      parameterSettings.put("apikey", "04490000a72fe7ec5cb3497f14e77f338c86f2fe");
      parameterSettings.put("licenseID", "g6h9zamsdtwhb93nc247ecrs");
      externalServicesFacade.setParameterSetting(parameterSettings);
      UIMABaseMetadataGenerator baseMetadataGenerator = new UIMABaseMetadataGenerator(externalServicesFacade);
      String textToAnalyze = TEXT_TO_ANALYZE;
      MGraph mGraph = new SimpleMGraph();
      GraphNode node = new GraphNode(new UriRef("test"), mGraph);
      baseMetadataGenerator.generate(node, textToAnalyze.getBytes(), MediaType.TEXT_PLAIN_TYPE);
    } catch (Exception e) {
      fail(e.getLocalizedMessage());
    }

  }

}
