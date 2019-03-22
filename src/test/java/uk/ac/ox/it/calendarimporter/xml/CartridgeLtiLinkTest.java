package uk.ac.ox.it.calendarimporter.xml;

import static org.junit.Assert.assertFalse;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import org.junit.Test;

public class CartridgeLtiLinkTest {

  @Test
  public void testToXml() throws JAXBException {
    JAXBContext contextA = JAXBContext.newInstance(CartridgeLtiLink.class);
    Marshaller marshaller = contextA.createMarshaller();
    marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
    StringWriter writer = new StringWriter();

    CartridgeLtiLink object = new CartridgeLtiLink();
    object.setTitle("Title");
    List<CartridgeLtiLink.Property> properties = new ArrayList<>();
    CartridgeLtiLink.Property property1 = new CartridgeLtiLink.Property();
    property1.setName("name1");
    property1.setValue("value1");
    properties.add(property1);
    CartridgeLtiLink.Property property2 = new CartridgeLtiLink.Property();
    property2.setName("name2");
    property2.setValue("value2");
    properties.add(property2);
    object.setProperties(properties);

    CartridgeLtiLink.Extensions extensions = new CartridgeLtiLink.Extensions();
    extensions.setPlatform("platform");
    CartridgeLtiLink.Options options = new CartridgeLtiLink.Options();
    options.setName("name");
    options.setProperties(properties);
    extensions.setOptions(Collections.singletonList(options));

    object.setExtensions(extensions);
    marshaller.marshal(object, writer);
    System.out.println(writer);
    assertFalse(writer.toString().isEmpty());
  }
}
