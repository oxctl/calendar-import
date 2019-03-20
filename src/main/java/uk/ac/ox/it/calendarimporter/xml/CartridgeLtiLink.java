package uk.ac.ox.it.calendarimporter.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;
import java.util.List;

/**
 * This is used to build the XML configuration file for the LTI tool.
 */
@XmlRootElement(name = "cartridge_basiclti_link", namespace = "http://www.imsglobal.org/xsd/imslticc_v1p0")
@XmlAccessorType(XmlAccessType.NONE)
public class CartridgeLtiLink {

    @XmlElement(name = "launch_url", namespace = "http://www.imsglobal.org/xsd/imsbasiclti_v1p0")
    private String launchUrl;

    @XmlElement(name = "title", namespace = "http://www.imsglobal.org/xsd/imsbasiclti_v1p0")
    private String title;

    @XmlElement(name = "description", namespace = "http://www.imsglobal.org/xsd/imsbasiclti_v1p0")
    private String description;

    @XmlElementWrapper(name = "custom", namespace = "http://www.imsglobal.org/xsd/imsbasiclti_v1p0")
    @XmlElement(name = "property", namespace = "http://www.imsglobal.org/xsd/imslticm_v1p0")
    private List<Property> properties;

    @XmlElement(name = "extensions", namespace = "http://www.imsglobal.org/xsd/imsbasiclti_v1p0")
    private Extensions extensions;

    public String getLaunchUrl() {
        return launchUrl;
    }

    public void setLaunchUrl(String launchUrl) {
        this.launchUrl = launchUrl;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Property> getProperties() {
        return properties;
    }

    public void setProperties(List<Property> properties) {
        this.properties = properties;
    }

    public Extensions getExtensions() {
        return extensions;
    }

    public void setExtensions(Extensions extensions) {
        this.extensions = extensions;
    }

    @XmlAccessorType(XmlAccessType.NONE)
    public static class Property {

        @XmlAttribute(name = "name")
        private String name;

        @XmlValue
        private String value;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    @XmlAccessorType(XmlAccessType.NONE)
    public static class Extensions {

        @XmlAttribute(name = "platform")
        private String platform;

        @XmlElement(name = "property", namespace = "http://www.imsglobal.org/xsd/imslticm_v1p0")
        private List<Property> properties;

        @XmlElement(name = "options", namespace = "http://www.imsglobal.org/xsd/imslticm_v1p0")
        private List<Options> options;

        public String getPlatform() {
            return platform;
        }

        public void setPlatform(String platform) {
            this.platform = platform;
        }

        public List<Property> getProperties() {
            return properties;
        }

        public void setProperties(List<Property> properties) {
            this.properties = properties;
        }

        public List<Options> getOptions() {
            return options;
        }

        public void setOptions(List<Options> options) {
            this.options = options;
        }
    }

    @XmlAccessorType(XmlAccessType.NONE)
    public static class Options {

        @XmlAttribute(name = "name")
        private String name;

        @XmlElement(name = "options", namespace = "http://www.imsglobal.org/xsd/imslticm_v1p0")
        private List<Property> properties;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<Property> getProperties() {
            return properties;
        }

        public void setProperties(List<Property> properties) {
            this.properties = properties;
        }
    }
}
