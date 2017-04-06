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
package com.raytheon.uf.common.datadelivery.retrieval.xml;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * DataSetBounds XML Object
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Apr 5, 2017  1045       tjensen     Initial creation
 *
 * </pre>
 *
 * @author tjensen
 */
@XmlAccessorType(XmlAccessType.NONE)
public class DataSetBounds {

    @XmlElement
    private double ulLat;

    @XmlElement
    private double ulLon;

    @XmlElement
    private double lrLat;

    @XmlElement
    private double lrLon;

    private List<Coordinate> coordList;

    public List<Coordinate> getCoordList() {
        if (coordList == null) {
            createCoordinates();
        }
        return coordList;
    }

    private void createCoordinates() {
        List<Coordinate> newCoords = new ArrayList<>(5);
        newCoords.add(new Coordinate(ulLon, ulLat));
        newCoords.add(new Coordinate(lrLon, ulLat));
        newCoords.add(new Coordinate(lrLon, lrLat));
        newCoords.add(new Coordinate(ulLon, lrLat));
        newCoords.add(newCoords.get(0));

        coordList = newCoords;
    }

    public void useDefaults() {
        ulLat = 90.0;
        ulLon = -180.0;
        lrLat = -90.0;
        lrLon = 180.0;
        createCoordinates();
    }

    public double getUlLat() {
        return ulLat;
    }

    public void setUlLat(double ulLat) {
        this.ulLat = ulLat;
        coordList = null;
    }

    public double getLrLat() {
        return lrLat;
    }

    public void setLrLat(double lrLat) {
        this.lrLat = lrLat;
        coordList = null;
    }

    public double getLrLon() {
        return lrLon;
    }

    public void setLrLon(double lrLon) {
        this.lrLon = lrLon;
        coordList = null;
    }

    public double getUlLon() {
        return ulLon;
    }

    public void setUlLon(double ulLon) {
        this.ulLon = ulLon;
        coordList = null;
    }
}
