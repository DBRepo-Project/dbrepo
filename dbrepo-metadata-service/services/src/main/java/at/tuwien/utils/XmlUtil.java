package at.tuwien.utils;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

public class XmlUtil {

    public static String pretty(String xmlString) {
        return pretty(xmlString, 2, true);
    }

    public static String pretty(String xmlString, int indent, boolean ignoreDeclaration) {
        xmlString = xmlString.replaceAll("(?m)^[ \t]*\r?\n", "").replaceAll("> <", "><");
        try {
            final InputSource src = new InputSource(new StringReader(xmlString.trim()));
            final Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(src);
            final TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setAttribute("indent-number", indent);
            final Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, ignoreDeclaration ? "yes" : "no");
            transformer.setOutputProperty(OutputKeys.INDENT, "np");
            final Writer out = new StringWriter();
            transformer.transform(new DOMSource(document), new StreamResult(out));
            return out.toString()
                    .trim();
        } catch (Exception e) {
            throw new RuntimeException("Error occurs when pretty-printing xml:\n" + xmlString, e);
        }
    }

}
