package at.ac.tuwien.ifs.dbrepo.utils;

import java.io.File;
import java.io.IOException;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.xml.sax.SAXException;

@Slf4j
public class XmlUtils {

    public static boolean validateXmlResponse(String xsdUrl, String xmlDocument) {
        try {

            /* xsd validation */
            final SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            final Schema schema = factory.newSchema(new File("src/test/resources/OAI-PMH.xsd"));
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
