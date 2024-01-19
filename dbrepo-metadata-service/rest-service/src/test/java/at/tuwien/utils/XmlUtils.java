package at.tuwien.utils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.xml.sax.SAXException;

@Log4j2
public class XmlUtils {

    public static boolean validateXmlResponse(String xsdUrl, String xmlDocument) {
        try {
            /* download schema */
            final File xsdFile = new File("./schema.xsd");
            FileUtils.copyURLToFile(new URL(xsdUrl), xsdFile);
            /* xsd validation */
            final SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            final Schema schema = factory.newSchema(xsdFile);
            final Validator validator = schema.newValidator();
            validator.validate(new StreamSource(IOUtils.toInputStream(xmlDocument)));
        } catch (IOException e) {
            log.error("Failed to download/read file: {}", e.getMessage());
            return false;
        } catch (SAXException e) {
            log.error("Failed to validate xml: {}", e.getMessage());
            return false;
        }
        return true;
    }
}
