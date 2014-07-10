package com.raytheon.uf.common.datadelivery.registry;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

@XmlRootElement(name = "encryption")
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize
public class Encryption {

    @XmlElement(name = "algorithim")
    @DynamicSerializeElement
    public Algorithim algorithim = Algorithim.AES;

    @XmlElement(name = "padding")
    @DynamicSerializeElement
    public Padding padding = Padding.AES;
    

    @XmlEnum
    public enum Algorithim {
        // CLEAR, No encryption
        // AES, AES encryption
        @XmlEnumValue(Algorithim.aes)
        AES("AES");

        private static final String aes = "AES";

        private final String algo;

        private Algorithim(String name) {
            algo = name;
        }

        @Override
        public String toString() {
            return algo;
        }
    }

    @XmlEnum
    public enum Padding {
        // CLEAR, No encryption
        // AES, AES encryption
        @XmlEnumValue(Padding.aes_pad)
        AES("AES/CFB8/NoPadding");

        private static final String aes_pad = "AES/CFB8/NoPadding";

        private final String padd;

        private Padding(String name) {
            padd = name;
        }

        @Override
        public String toString() {
            return padd;
        }
    }
    /**
     * @return algorithm
     */
    public Algorithim getAlgorithim() {
        return algorithim;
    }

    /**
     * @param algorithim
     *            the algorithim to set
     */
    public void setAlgorithim(Algorithim algorithim) {
        this.algorithim = algorithim;
    }

    /**
     * @return the padding
     */
    public Padding getPadding() {
        return padding;
    }

    /**
     * @param padding
     *            the padding to set
     */
    public void setPadding(Padding padding) {
        this.padding = padding;
    }

}
