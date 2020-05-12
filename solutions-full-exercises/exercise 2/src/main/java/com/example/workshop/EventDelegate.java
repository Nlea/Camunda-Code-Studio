package com.example.workshop;

import java.io.StringReader;
import java.util.logging.Logger;

import javax.inject.Named;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.mashape.unirest.http.Unirest;

   

@Named 
public class EventDelegate implements JavaDelegate {
	

	  private final Logger LOGGER = Logger.getLogger(EventDelegate.class.getName());
	  
	  public void execute(DelegateExecution execution) throws Exception {
		  
		  // Make the REST call	
		  String response = Unirest.get("http://www.nyartbeat.com/list/event_searchNear?latitude=40.719130&longitude=-73.980000")
				  .header("Cache-Control", "no-cache")
				  .header("Postman-Token", "d6b46668-4251-416e-a6c7-06ab2d53bc2e")
				  .asString()
				  .getBody();
		  
		  //Parse the XML
	
			  DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			  DocumentBuilder builder = factory.newDocumentBuilder();
			  
			  Document doc = builder.parse(new InputSource(new StringReader(response)));
			  doc.getDocumentElement().normalize();
			  
			  //Store Events into a list
			  NodeList nList = doc.getElementsByTagName("Event");
			  int numberEvents = 0;

			  //Iterate over the list			  
			  for (int temp = 0; temp < nList.getLength(); temp++){
numberEvents++;
		          Node node = nList.item(temp);
		          if (node.getNodeType() == Node.ELEMENT_NODE) {
		             //Save event information and set Variables back to the process
		             Element eElement = (Element) node;		             
		             String name = eElement.getElementsByTagName("Name").item(0).getTextContent();
		             String type = eElement.getElementsByTagName("Type").item(0).getTextContent();
		             execution.setVariable("name"+ temp, name);
		             execution.setVariable("type"+ temp, type);
		             }
		          }
execution.setVariable("number", numberEvents);
			  }
}
