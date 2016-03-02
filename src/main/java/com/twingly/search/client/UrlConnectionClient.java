package com.twingly.search.client;

import com.twingly.search.Constants;
import com.twingly.search.Post;
import com.twingly.search.Result;
import com.twingly.search.exception.BlogStream;
import com.twingly.search.exception.OperationResult;
import com.twingly.search.exception.TwinglyException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 * Client that could be used in Query object.
 * This client uses URLConnection to perform network interactions.
 * UrlConnectionClient is used by default in Query object
 *
 * @see Client
 * @see com.twingly.search.Query
 * @see URLConnection
 */
public class UrlConnectionClient implements Client {
    /**
     * The constant USER_AGENT_PROPERTY.
     */
    protected static final String USER_AGENT_PROPERTY = "User-Agent";
    private static final String DEFAULT_USER_AGENT = "Twingly Search Java Client/" + Constants.VERSION;
    private String userAgent = DEFAULT_USER_AGENT;
    private JAXBContext jaxbContext;

    /**
     * {@inheritDoc}
     */
    public String getUserAgent() {
        return userAgent;
    }

    /**
     * {@inheritDoc}
     */
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }


    /**
     * Gets jaxb context.
     *
     * @return the jaxb context
     */
    protected JAXBContext getJAXBContext() {
        if (jaxbContext == null) {
            try {
                jaxbContext = JAXBContext.newInstance(Result.class, Post.class, OperationResult.class, BlogStream.class);
            } catch (JAXBException e) {
                throw new TwinglyException("Cannot initialize JAXBContext for Result", e);
            }
        }
        return jaxbContext;
    }

    /**
     * Unmarshal xml for result.
     *
     * @param reader the reader
     * @return the result
     */
    protected Result unmarshalXmlForResult(Reader reader) {
        try {
            Unmarshaller jaxbUnmarshaller = getJAXBContext().createUnmarshaller();
            Object result = jaxbUnmarshaller.unmarshal(reader);
            if (result instanceof Result) {
                return (Result) result;
            } else if (result instanceof BlogStream) {
                throw new TwinglyException((BlogStream) result);
            }
            throw new TwinglyException("Unprocessed exception");
        } catch (JAXBException e) {
            throw new TwinglyException("Unable to process request", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Result makeRequest(String query) {
        try {
            URL url = getUrl(query);
            URLConnection connection = url.openConnection();
            connection.setRequestProperty(USER_AGENT_PROPERTY, getUserAgent());
            try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                return unmarshalXmlForResult(br);
            }
        } catch (IOException e) {
            throw new TwinglyException("IO exception", e);
        }
    }

    /**
     * Gets url from query string.
     *
     * @param query the query
     * @return the url
     */
    protected URL getUrl(String query) {
        try {
            return new URL(query);
        } catch (MalformedURLException e) {
            throw new TwinglyException("Malformed query", e);
        }
    }
}
