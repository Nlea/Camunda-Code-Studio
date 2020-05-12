# Camunda Workshop: Expecting the unexpecting
Welcome to this Camunda Workshop. This workshop was designed as part of the Camundacon in New York 2020. Anyhow you should be able to follow the exercises without attending the workshop. The readme contains the exercise instruction. It should be easy to follow. In the other folders you find the model solutions as well as the full code solutions. The presentation from the workshop is provided as well.


To make that an even better learning experience Pull request are very welcome! 



## Exercise 1: Set up a Camunda Springboot appliaction
:trophy: Goal of exercise number 1 is to create a running Camunda instance with Springboot.


Luckily we have the [Camunda BPM Intitializer](https://start.camunda.com/), which will create a plain Camunda Springboot project for us. 

![Camunda-BPM-Intitializer-set-up](/img/Camunda-BPM-Intializer.png)

If you want to create the project manually and understand what the Camunda BPM Intitializer does, this [tutorial](https://docs.camunda.org/get-started/spring-boot/) guides you through it. 


By generating your project you will download a zip file that contains a pom file with the needed dependencies as well as a simple application class in order to start Camunda as a spring boot application. Extract the file and import the project to an IDE of your choice (e.g.: in Eclipse: import -> existing maven project -> select unzipped folder)

![imported Mavenproject](/img/imported-project.png)


Navigate to scr/main/java/com.example.workshop/Application.java. right click on the class and run it on your server. Springboot will start up Camunda. Visit localhost:8080 and login with the credentials you have create in the Camunda BPM Intializer. 

![Camunda-is-running](/img/Camunda-is-running.png)

**:tada: Congrats you have a Camunda running** 




## Exercise 2: Add a Process model to your application
:trophy: The goal of this exercise is to create a process for our application. 

For this exercise we need the Camunda Modeler. If you don't have it already download it [here](https://camunda.com/download/modeler/)


Next you can see in your Mavenproject under scr/main/ressource a file called process.bpmn open that process in the Camunda Modeler. Now we need to change it to the following: 

:pencil2:
We want to get a bucket list for things that we can do while we are all in New York. Therefore we need to get information about on going events. Luckily there is an API that we can call to get information on ongoing events in New York. After doing that we want to book tickets automatically and after that display the event information to a user. 


Model the process. *Hint: [Here](/solution-bpmn-models) you find the modeling solutions for the exercises.* 
Save your model. 


In order to make it run within Camunda we need to add the technical details. That means we need to provide code for the two Service tasks. In the next steps we will write the EventDelegate class and the BookTicketDelegate and connect it to our model. 


First we take care of the Task, that will get us information from the New York Event API. Therefore we select the task in the modeler and open the properties panel in the modeler. We want to implement the task with a delegate Expression: 

![EventDelegate](/img/implement-EventDelegate.png)


The first code, that we write needs to make a REST call. Therefore we need to add the unirest dependency to our pom file:

```
<dependency>
    <groupId>com.mashape.unirest</groupId>
    <artifactId>unirest-java</artifactId>
    <version>1.4.9</version>
 </dependency> 
```
After making the REST call we need to parse the XML. For that we need to inculde the following dependencies to our pom file: 
```
<dependency>
    <groupId>org.owasp</groupId>
    <artifactId>java-file-io</artifactId>
    <version>1.0.0</version>
</dependency>

<dependency>
    <groupId>javax.xml.parsers</groupId>
    <artifactId>jaxp-api</artifactId>
    <version>1.4.5</version>
</dependency>

<dependency>
    <groupId>org.w3c</groupId>
    <artifactId>dom</artifactId>
    <version>2.3.0-jaxb-1.0.6</version>
</dependency>
```

Now we need to create a new class in our Mavenproject. Create a new class "EventDelegate" in the package com.example.workshop. This class will call the event API and will provide us the data for our process. Do write the Java class you can copy the code below: 

```java
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
```
Okay so for this workshop we don't want to book the tickets for real. Therefore we will implement the "Book ticket" task with a simple LogggerDelegate that will lock some information to our console. Select the service task in your model and choose for the implementation: Delegate Expression. The expression is the following: 
> #{loggerDelegate}

Create a new LoggerDelegate class in your package com.camunda.workshop. You can use the following code for the implementation: 
```Java
package com.example.workshop;

import java.util.logging.Logger;

import javax.inject.Named;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;

/**
 * This is an easy adapter implementation 
 * illustrating how a Java Delegate can be used 
 * from within a BPMN 2.0 Service Task.
 */
@Named
public class LoggerDelegate implements JavaDelegate {
 
  private final Logger LOGGER = Logger.getLogger(LoggerDelegate.class.getName());
  
  public void execute(DelegateExecution execution) throws Exception {
    
    LOGGER.info("\n\n  ... LoggerDelegate invoked by "
            + "processDefinitionId=" + execution.getProcessDefinitionId()
            + ", activtyId=" + execution.getCurrentActivityId()
            + ", activtyName='" + execution.getCurrentActivityName() + "'"
            + ", processInstanceId=" + execution.getProcessInstanceId()
            + ", businessKey=" + execution.getProcessBusinessKey()
            + ", executionId=" + execution.getId()
            + " \n\n");
    
  }
}
```



Restart your springboot application. Now we have Camunda with a deployed process running. Navigate to Tasklist and start your process. You can navigate between the different Camunda frontend applications by clicking on the house symbole in the upper right corner

![Navigation](/img/Navigation-Camunda-webapps.png). 

In Tasklist you can start your process. 

![Start a process](/img/start-a-process.png). 

Navigate back to Cockpit and have a look at your process overview. You will know see that you have one running instance on your process

![Process overview](/img/Cockpit-process.png)

If you click on that process Name you will get to a view, where you can see your model. Every active instance is presented with a blue token. 

![](/img/running-instance.png)

By selecting the ID of your instance you can get further information about it like for example the process variables. 


Our instance now stands on the user task. Let's complete that task and finish the instance. Navigate back to tasklist. Currently we don't see any tasks there. That is because we have not create a query for the avaidable tasks from the engine. By clicking on "simple filter" we create a query that will display us all tasks. 

![Filter](/img/simple-filter.png)

Now we can see the task. In order to complete it: 
1. We need to claim it
2. As we did not provide any form implementation for the user, we don't get any information in the task. Click on Load variables in order to examine the information from the Servcie task
3. Complete the task 

![Taskmanagament](/img/display-events-task.png)

**:tada: Congrats your first process in Camunda runs and you have completed one instance of it** 


## Exercise 3: Use process data to route your process
:trophy: The goal of this exercise is to use the data from the Service task to route your process. 


:pencil2: For using the data, we need to adjust our process model: Our current process has the danger of booking a lot of tickets for events depending on our first REST call. So if we get more than two events back, we want our user to choose the events he or she likes to visit before we book the tickets. Adjust the process accordingly. *Hint: [Here](/solution-bpmn-models) you find the modeling solutions for the exercises.* 

Now we need to implement the technical details for the new version of the model. First we need to pick the Sequence flows after the XOR Gateway. The EventDelegate class will count the number of events we have found and store in the process variable numbers. 

We can use following expression for having less than 3 events and still make sure that we have at least one event, because other than that our booking class would fail: 

> #{number > 0 && number < 3}

![](/img/XORgateway.png)

Select the other sequence flow as well and use the following expression: 

> #{number >= 3}

The expression language used is from Java the Unified Expression Language (UEL). If you want to find an overview and more information you can look [here](https://docs.oracle.com/javaee/5/tutorial/doc/bnahq.html).

In this workshop we won't take care of implementing the user task to select the events. Just start the process application again and route through your process. 


**:tada: Congrats now your process can be routed depending the data you get** 


## Exercise 4: Handeling unexpected data
:trophy: Goal of exercise number 4 is to understand how Camunda deals with problems due to missing data or wrong data. 


Let's assume our EventDelegate will set the number variable to 0. Comment the line in your code: 
> //numberEvents++;

Restart your springboot application. What do you expect? Start your process. 


Right now starting the process will throw an execption for the user that starts the process, because the expression at the gateway cannot be evaluated. In Cockpit is looks like that the instance never has been started. This is due to Camunda BPM transaction boundaries (which we have discussed in the workshop presentation). We can set those boundaries manually in order to handel missing data or failure of services better. 

Set in the properties panel for the "Get NY event information task" an
[x]asynchronous after 
Run your application again and start a new process. Inspect Cockpit. Now we can see that we have an incident in our process. Here we can inspect the incidence. The incidents we produce here is not a failure of the service task. The incidents occures because the process gets data that can't be handeled.   


:pencil2: Some expecting problems we would like to handle on the businessside. Therefore we can use the concept of BPMN errors. In our scenario we don't want an administrator to fix the problem, when the API will return 0 events. In those cases we don't want to reach the XOR gateway but through an error and let the user look for alternatives. Adjust your process model accordingly. 

Now let's have a look into the code for the EventDelegate. Here we have to make sure that we throw the BPMN error as well. So we just set the variable number if it is bigger than 0. Otherwise we will throw the BPMN error. 


```Java
if (numberEvents > 0) {
				  execution.setVariable("number", numberEvents);
			  } else {
				  throw new BpmnError("NoEventER");
			  }
```


## Exercise 5: Add more flexibility to your process
:trophy: Goal of exercise is to add more flexibility to your model and deal with roll back situation. 


:pencil2: Our NY bucket list process is almost perfect. We just missed the fact that everyone has a good friend living in New York. Of course as soon as we would receive a message from our friend after we started the process we still want to go out with our friend and if we have booked any tickets yet, we want to make sure we canlce them. Adjust the model accordingly. 


Set the technical attributes in the model. First we need to define a message for the start event in the event subprocess: 

IMG

Now decide on the compensate end event which tasks should be compensated

IMG

For the compensation task we need to choose an implementation as well. As we haven't booked anything we can here use the LoggerDelegate again. 

IMG

Finally uncomment: 
> numberEvents++;

Restart your application and start the process again. It should be running and should wait at the user task "Select galleries". Complete the task now it should wait at the task "Display events". 

Let's send via REST call the invitation message. If you already have a Tool to send REST calls you can use it. Or you can get [Postman here](https://www.postman.com/). Now let's send a message to our Camunda Engine: 

> POST http://localhost:8080/engine/message

The body of the REST call should look like this

```JSON
{
  "messageName": "invitationMSG",
  "resultEnabled": "true",
  "processVariables" : {
    "invitationtext" : {"value" : "Hey let's meet today for lunch and explore the city afterwards together", "type": "String"}
  }
}
```
More detailed information on the message REST call can be found [here](https://docs.camunda.org/manual/latest/reference/rest/message/post-message/#example). Of course that is just a small part of [Camunda's REST API](https://docs.camunda.org/manual/latest/reference/rest/). Maybe you can use the docs to find out how to start an instance of your process via REST. 

Back to our current running instance: 

Finish the user task "Agree on meeting point" and observer your console and Cockpit. 
