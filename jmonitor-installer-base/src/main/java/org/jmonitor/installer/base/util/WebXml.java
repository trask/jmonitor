/*
 * Copyright 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jmonitor.installer.base.util;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Adds content to web.xml while preserving existing formatting.
 * 
 * @author Trask Stalnaker
 * @since 1.0
 */
public class WebXml {

    private final String newline;
    private final Document document;

    public WebXml(String text) {

        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            document = builder.parse(new InputSource(new StringReader(text)));
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException(e);
        } catch (SAXException e) {
            throw new IllegalStateException(e);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        // try to keep consistent newline in additions to web.xml
        if (text.contains("\r\n")) {
            newline = "\n";
        } else {
            newline = "\n";
        }
    }

    public void addFilter(String filterName, String filterClass, String aboveComment, String belowComment) {

        Node insertionPointNode =
                findFirstMatchingElement(document, Arrays.asList("filter", "listener", "servlet"));

        if (insertionPointNode == null) {
            // web.xml is pretty useless without any <filter> or <servlet> tags
            throw new IllegalStateException("web.xml doesn't have <filter> or <servlet> tag");
        }

        String[] indentations =
                getIndentationsFromFirstMatchingElement(document,
                        Arrays.asList("filter", "servlet"));

        XmlDocumentInsertion documentInsertion =
                new XmlDocumentInsertion(document, insertionPointNode, newline, indentations[0]);

        if (aboveComment != null) {
            documentInsertion.addComment(aboveComment);
        }

        Element filterElement = documentInsertion.addElement("filter");
        documentInsertion.addElement("filter-name", filterName, indentations[1], filterElement);
        documentInsertion.addElement("filter-class", filterClass, indentations[1], filterElement);

        if (belowComment != null) {
            documentInsertion.addComment(belowComment);
        }
    }

    public void addFilterMapping(String filterName, String urlPattern, String aboveComment, String belowComment) {

        Node insertionPointNode =
                findFirstMatchingElement(document, Arrays.asList("filter-mapping", "listener",
                        "servlet"));

        if (insertionPointNode == null) {
            // web.xml is pretty useless without any <filter-mapping> or <servlet-mapping> tags
            throw new IllegalStateException(
                    "web.xml doesn't have <filter-mapping> or <servlet> tag");
        }

        String[] indentations =
                getIndentationsFromFirstMatchingElement(document,
                        Arrays.asList("filter-mapping", "servlet-mapping"));

        XmlDocumentInsertion documentInsertion =
                new XmlDocumentInsertion(document, insertionPointNode, newline, indentations[0]);

        if (aboveComment != null) {
            documentInsertion.addComment(aboveComment);
        }

        Element filterMappingElement = documentInsertion.addElement("filter-mapping");
        documentInsertion.addElement("filter-name", filterName, indentations[1],
                filterMappingElement);
        documentInsertion.addElement("url-pattern", urlPattern, indentations[1],
                filterMappingElement);

        if (belowComment != null) {
            documentInsertion.addComment(belowComment);
        }
    }

    private Element findFirstMatchingElement(Document document, List<String> tagNames) {

        Element webAppElement = document.getDocumentElement();
        for (int i = 0; i < webAppElement.getChildNodes().getLength(); i++) {

            Node childNode = webAppElement.getChildNodes().item(i);

            if (childNode instanceof Element
                    && tagNames.contains(((Element) childNode).getTagName())) {

                return (Element) childNode;
            }
        }
        return null;
    }

    private String[] getIndentationsFromFirstMatchingElement(Document document,
            List<String> tagNames) {

        Element webAppElement = document.getDocumentElement();
        for (int i = 0; i < webAppElement.getChildNodes().getLength(); i++) {

            Node childNode = webAppElement.getChildNodes().item(i);

            if (childNode instanceof Element
                    && tagNames.contains(((Element) childNode).getTagName())) {

                Node previousNode = webAppElement.getChildNodes().item(i - 1);
                String zeroLevelIndentation = getIndentation(previousNode);
                String indentation;
                if (childNode.hasChildNodes()) {
                    indentation = getIndentation(childNode.getChildNodes().item(0));
                } else {
                    // not sure how this could occur in our case
                    // <servlet>, <filter>, <servlet-mapping>, <filter-mapping> tags should all have children
                    indentation = "";
                }
                indentation = StringUtils.substringAfter(indentation, zeroLevelIndentation);
                return new String[] {zeroLevelIndentation, indentation};
            }
        }
        throw new IllegalStateException(
                "This should only be called after calling findFirstMatchingElement()");
    }

    private String getIndentation(Node previousNode) {
        if (previousNode instanceof Text) {
            String textContent = ((Text) previousNode).getTextContent();
            // (?s) enables DOTALL mode so that . matches everything, including newlines
            if (textContent.matches("(?s).*\n *")) {
                return StringUtils.substringAfterLast(textContent, "\n");
            }
        }
        return "";
    }

    @Override
    public String toString() {
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(document), new StreamResult(writer));
            return writer.toString();
        } catch (TransformerConfigurationException e) {
            throw new IllegalStateException(e);
        } catch (TransformerFactoryConfigurationError e) {
            throw new IllegalStateException(e);
        } catch (TransformerException e) {
            throw new IllegalStateException(e);
        }
    }
}
