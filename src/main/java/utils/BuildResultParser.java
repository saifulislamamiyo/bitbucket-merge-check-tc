package utils;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

public class BuildResultParser {

    //parsing build results, mainly used to check <buildType></buildType> outputs
    @SuppressWarnings({"unchecked", "restriction"})
    public ArrayList<ModelBuild> parseBuildReults(InputStreamReader reader) {

        ArrayList<ModelBuild> models = new ArrayList<ModelBuild>();
        try {
            XMLInputFactory factory = XMLInputFactory.newInstance();
            XMLEventReader eventReader = factory.createXMLEventReader(reader);
            String tempBuildName = null;

            while (eventReader.hasNext()) {
                XMLEvent event = eventReader.nextEvent();

                switch (event.getEventType()) {

                    case XMLStreamConstants.START_ELEMENT:
                        StartElement startElement = event.asStartElement();
                        String qName = startElement.getName().getLocalPart();

                        if (qName.equalsIgnoreCase("buildType")) {
                            Iterator<Attribute> attributes = startElement.getAttributes();
                            while (attributes.hasNext()) {
                                Attribute a = attributes.next();
                                if ("name".equalsIgnoreCase(a.getName().getLocalPart())) {
                                    tempBuildName = a.getValue();
                                    break;
                                }
                            }
                        } else if (qName.equalsIgnoreCase("build")) {
                            ModelBuild m = new ModelBuild();
                            Iterator<Attribute> attributes = startElement.getAttributes();
                            m.setBuildName(tempBuildName);
                            while (attributes.hasNext()) {
                                Attribute a = attributes.next();
                                // add the temporary build name as this model's build name
                                if ("number".equalsIgnoreCase(a.getName().getLocalPart())) {
                                    m.setRevNumber(a.getValue());
                                } else if ("status".equalsIgnoreCase(a.getName().getLocalPart())) {
                                    m.setStatus(a.getValue());
                                } else if ("state".equalsIgnoreCase(a.getName().getLocalPart())) {
                                    m.setState(a.getValue());
                                } else if ("webUrl".equalsIgnoreCase(a.getName().getLocalPart())) {
                                    m.setWebUrl(a.getValue());
                                } 
                            }
                            models.add(m);
                        }
                        break;

                    case XMLStreamConstants.CHARACTERS:
                        // Characters characters = event.asCharacters();
                        // if (!characters.isWhiteSpace()) {
                        // System.out.println("Data: " + characters.getData());
                        //
                        // }
                        break;

                    case XMLStreamConstants.END_ELEMENT:
                        break;
                }
            }
        } catch (XMLStreamException e) {
            models = null;
            e.printStackTrace();
        }
        return models;

    }

    //parsing build results, mainly used to check <build></build> outputs
    public String parseQueueResults(InputStreamReader reader) {
        String count = null;
        try {
            XMLInputFactory factory = XMLInputFactory.newInstance();
            XMLEventReader eventReader = factory.createXMLEventReader(reader);
            while (eventReader.hasNext()) {
                XMLEvent event = eventReader.nextEvent();
                switch (event.getEventType()) {

                    case XMLStreamConstants.START_ELEMENT:
                        StartElement startElement = event.asStartElement();
                        String qName = startElement.getName().getLocalPart();

                        if (qName.equalsIgnoreCase("builds")) {
                            Iterator<Attribute> attributes = startElement.getAttributes();
                            while (attributes.hasNext()) {
                                Attribute a = attributes.next();
                                if ("count".equalsIgnoreCase(a.getName().getLocalPart())) {
                                    count = a.getValue();
                                    break;
                                }
                            }
                        }

                    case XMLStreamConstants.CHARACTERS:
                        // Characters characters = event.asCharacters();
                        // if (!characters.isWhiteSpace()) {
                        // System.out.println("Data: " + characters.getData());
                        //
                        // }
                        break;

                    case XMLStreamConstants.END_ELEMENT:
                        break;
                }
            }
        } catch (XMLStreamException e) {
            count = "0";
            e.printStackTrace();
        }
        return count;
    }

    public void toString(ArrayList<ModelBuild> models) {
        for (ModelBuild modelBuild : models) {
            System.out.println(modelBuild.toString());
        }
    }

}
