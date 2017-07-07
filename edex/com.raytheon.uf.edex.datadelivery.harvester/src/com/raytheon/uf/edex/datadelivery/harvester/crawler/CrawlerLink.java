/**
 * This software was developed and / or modified by Raytheon Company,
 * pursuant to Contract DG133W-05-CQ-1067 with the US Government.
 *
 * U.S. EXPORT CONTROLLED TECHNICAL DATA
 * This software product contains export-restricted data whose
 * export/transfer/disclosure is restricted by U.S. law. Dissemination
 * to non-U.S. persons whether in the United States or abroad requires
 * an export license or other authorization.
 *
 * Contractor Name:        Raytheon Company
 * Contractor Address:     6825 Pine Street, Suite 340
 *                         Mail Stop B8
 *                         Omaha, NE 68106
 *                         402.291.0100
 *
 * See the AWIPS II Master Rights File ("Master Rights File.pdf") for
 * further licensing information.
 **/
package com.raytheon.uf.edex.datadelivery.harvester.crawler;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Index;

import com.raytheon.uf.common.dataplugin.persist.PersistableDataObject;
import com.raytheon.uf.edex.datadelivery.retrieval.metadata.Link;

import opendap.dap.DAS;

/**
 * Crawler link
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jul 7, 2017  6178      tgurney     Initial creation
 *
 * </pre>
 *
 * @author tgurney
 */

@Entity
@Table(name = "crawler_link")
@org.hibernate.annotations.Table(appliesTo = "crawler_link", indexes = {
        @Index(name = "crawler_link_providerNameCollectionNameIdx", columnNames = {
                "providerName", "collectionName" }) })
public class CrawlerLink extends PersistableDataObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(nullable = false, length = 256)
    private String url;

    @Column(nullable = false)
    private boolean processed;

    @Column(nullable = false, length = 128)
    private String providerName;

    @Column(nullable = false, length = 128)
    private String collectionName;

    @Column(nullable = false, length = 128)
    private String subName;

    @Index(name = "crawler_link_creationTimeIdx")
    @Column(nullable = false)
    private long creationTime = System.currentTimeMillis();

    @Transient
    private Map<String, DAS> metadata = new HashMap<>();

    public CrawlerLink() {
    }

    /**
     * Copy constructor. Always sets creationTime to the current time and
     * processed to false, regardless of their values on the original
     */
    public CrawlerLink(CrawlerLink link) {
        this.collectionName = link.collectionName;
        this.creationTime = System.currentTimeMillis();
        this.subName = link.subName;
        this.collectionName = link.collectionName;
        this.providerName = link.providerName;
        this.processed = false;
        this.url = link.url;
        this.metadata = link.metadata;
    }

    @Override
    public String getIdentifier() {
        return url;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public boolean isProcessed() {
        return processed;
    }

    public void setProcessed(boolean processed) {
        this.processed = processed;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public String getCollectionName() {
        return collectionName;
    }

    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public String getSubName() {
        return subName;
    }

    public void setSubName(String subName) {
        this.subName = subName;
    }

    public void setCreationTime(long creationTime) {
        this.creationTime = creationTime;
    }

    public Map<String, DAS> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, DAS> metadata) {
        this.metadata = metadata;
    }

    public Link asLink() {
        Link link = new Link();
        link.setMetadata(metadata);
        link.setSubName(subName);
        link.setUrl(url);
        return link;
    }

}
