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

import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

/**
 * 
 * 
 * @author Trask Stalnaker
 * @since 1.0
 */
public class XmlDocumentInsertion {

    private final Document document;
    private final Node insertionPointNode;
    private final String newline;
    // the "zero indentation" is the indentation of the insertion point node
    private final String zeroIndentation;

    public XmlDocumentInsertion(Document document, Node insertionPointNode, String newline,
            String zeroIndentation) {

        this.document = document;
        this.insertionPointNode = insertionPointNode;
        this.newline = newline;
        this.zeroIndentation = zeroIndentation;
    }

    public Comment addComment(String data) {
        return addComment(data, null, null);
    }

    public Comment addComment(String data, String indentation, Element parentElement) {

        addIndent(indentation, parentElement);
        Comment comment = document.createComment(" " + data + " ");
        add(comment, parentElement);
        addNewlineAndZeroIndent(parentElement);
        return comment;
    }

    public Element addElement(String tagName) {
        Element element = addElement(tagName, null);
        addNewlineAndZeroIndent(element);
        return element;
    }

    public Element addElement(String tagName, String data) {
        return addElement(tagName, data, null, null);
    }

    public Element addElement(String tagName, String data, String indentation,
            Element parentElement) {

        addIndent(indentation, parentElement);
        Element element = document.createElement(tagName);
        add(element, parentElement);
        addText(data, element);
        addNewlineAndZeroIndent(parentElement);
        return element;
    }

    private void addNewlineAndZeroIndent(Element parentElement) {
        addText(newline + zeroIndentation, parentElement);
    }

    private void addIndent(String indentation, Element parentElement) {
        addText(indentation, parentElement);
    }

    private void addText(String data, Element parentElement) {
        if (data != null) {
            Text text = document.createTextNode(data);
            add(text, parentElement);
        }
    }

    private void add(Node node, Element parentElement) {
        if (parentElement == null) {
            insertionPointNode.getParentNode().insertBefore(node, insertionPointNode);
        } else {
            parentElement.appendChild(node);
        }
    }
}
