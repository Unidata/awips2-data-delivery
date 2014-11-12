package com.raytheon.uf.common.comm;

import java.io.PrintWriter;
import java.io.StringReader;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

/**
 *
 * Test HTTPS connection servlet for catalog requests
 */
public class CatalogServlet extends HttpServlet {

    private HttpServletRequest request;
    private HttpServletResponse response;
    private PrintWriter out;

    public CatalogServlet() {
        
    }

    @Override
    protected void doPost(HttpServletRequest request,
            HttpServletResponse response) {
        try {
            this.request = request;
            this.response = response;
            this.response.setContentType("text/xml");
            this.out = this.response.getWriter();

            String xmlContent = request.getParameter("xml");

            // Create a document builder.
            DocumentBuilder db = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder();
            InputSource is = new InputSource();

            // Load the XML data into an InputSource object.
            is.setCharacterStream(new StringReader(xmlContent));

            // Parse the raw data into a Document object.
            Document doc = db.parse(is);

            if (xmlContent != null) {
                out.print(xmlContent);
            } else {
                // XML invalid
                out.print("xml invalid");
            }
        } catch (Exception ex) {
            out.print(ex.getMessage());
        }
    }
}
