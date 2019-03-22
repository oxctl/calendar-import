@XmlSchema(
    namespace = "http://www.imsglobal.org/xsd/imslticc_v1p0",
    elementFormDefault = XmlNsForm.QUALIFIED,
    xmlns = {
      @XmlNs(prefix = "", namespaceURI = "http://www.imsglobal.org/xsd/imslticc_v1p0"),
      @XmlNs(prefix = "blti", namespaceURI = "http://www.imsglobal.org/xsd/imsbasiclti_v1p0"),
      @XmlNs(prefix = "lticm", namespaceURI = "http://www.imsglobal.org/xsd/imslticm_v1p0"),
      @XmlNs(prefix = "lticp", namespaceURI = "http://www.imsglobal.org/xsd/imslticp_v1p0")
    })
package uk.ac.ox.it.calendarimporter.xml;

import javax.xml.bind.annotation.XmlNs;
import javax.xml.bind.annotation.XmlNsForm;
import javax.xml.bind.annotation.XmlSchema;
