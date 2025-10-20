package se.replyto.hackathon.routbuilders;

import org.apache.camel.CamelContext;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.errorhandler.DefaultErrorHandlerDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class RoutBuilder01 extends RouteBuilder {

	final String loggerId = getClass().getName();
	
	@Autowired
	Environment env;

	@Autowired
	CamelContext context;

	@Override
	public void configure(){
		context.setMessageHistory(true);
		context.setSourceLocationEnabled(true);

		final DefaultErrorHandlerDefinition deadLetterChannelBuilder =
				deadLetterChannel("direct:error-handler")
				.retryAttemptedLogLevel(LoggingLevel.WARN)
				.useOriginalMessage()
				.maximumRedeliveries(1)
				.redeliveryDelay(1000);
		
		errorHandler(noErrorHandler());
	    
		// Main route ---------------------------------------------------------
		from("file:demo/int001-in?antInclude=*.txt")
			.routeId("int001-exercise-main-route")
			.errorHandler(deadLetterChannelBuilder)

			// File received from inbound endpoint
			.log(LoggingLevel.INFO, loggerId, "Incoming file headers: ${headers}")
			.log(LoggingLevel.INFO, loggerId, """
			  Incoming file body:
			  ${body}
			  """)

			// Send file to outbound endpoint
			.to("file:demo/int001-out?fileExist=Fail")
			.log(LoggingLevel.INFO, loggerId, "Outgoing file headers: ${headers}")
			.log(LoggingLevel.INFO, loggerId, "Sent file body: " + System.lineSeparator() + "${bodyAs(String)}");
		

		// Error handling route -----------------------------------------------
		from("direct:error-handler")
			.routeId("error-handler-route")
			.log(LoggingLevel.WARN, "Moving ${header.CamelFileName} to backout folder {{int001.backout.uri}}...")
			.to("{{int001.backout.uri}}")
			.log(LoggingLevel.WARN, "Moved ${header.CamelFileName} to backout folder {{int001.backout.uri}}")
			.log(LoggingLevel.ERROR, """
									
				E R R O R   R E P O R T :
				---------------------------------------------------------------------------------------------------------------------------------------
				
				Failure Route ID: ${exchangeProperty.CamelFailureRouteId}
				Failure Endpoint: ${exchangeProperty.CamelFailureEndpoint}

				Exception Type:   ${exception.class.name}
				Exception Message: ${exception.message}
				
				Stacktrace: ${exception.stacktrace}
				${messageHistory}
				""");
	}

}