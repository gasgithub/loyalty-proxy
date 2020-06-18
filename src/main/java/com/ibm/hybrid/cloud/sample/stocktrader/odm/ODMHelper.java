package com.ibm.hybrid.cloud.sample.stocktrader.odm;

import java.util.Base64;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.jms.JMSException;
import javax.naming.NamingException;
import javax.servlet.http.HttpServletRequest;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.opentracing.Traced;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import com.ibm.hybrid.cloud.sample.stocktrader.loyalty.LoyaltyChange;
import com.ibm.hybrid.cloud.sample.stocktrader.odm.ODMLoyaltyRule;
import com.ibm.hybrid.cloud.sample.stocktrader.odm.ODMClient;


public class ODMHelper {
	private static Logger logger = Logger.getLogger(ODMHelper.class.getName());
	
	private @Inject @ConfigProperty(name = "ODM_ID", defaultValue = "odmAdmin") String odmId;
	private @Inject @ConfigProperty(name = "ODM_PWD", defaultValue = "odmAdmin") String odmPwd;
	private @Inject @RestClient ODMClient odmClient;

	public ODMHelper() {
		// TODO Auto-generated constructor stub
	}
	
	@Traced
	public String invokeODM(String owner, double overallTotal, String oldLoyalty) {
		String loyalty = null;
		ODMLoyaltyRule input = new ODMLoyaltyRule(overallTotal);
   		String credentials = odmId+":"+odmPwd;
			String basicAuth = "Basic "+Base64.getEncoder().encode(credentials.getBytes());

			//call the LoyaltyLevel business rule to get the current loyalty level of this portfolio
			logger.info("Calling loyalty-level ODM business rule for "+owner);
			ODMLoyaltyRule result = odmClient.getLoyaltyLevel(basicAuth, input);

			loyalty = result.determineLoyalty();
			logger.info("New loyalty level for "+owner+" is "+loyalty);

			
			/*
			 * if (!oldLoyalty.equalsIgnoreCase(loyalty)) try {
			 * logger.info("Change in loyalty level detected.");
			 * 
			 * LoyaltyChange message = new LoyaltyChange(owner, oldLoyalty, loyalty);
			 * 
			 * String user = request.getRemoteUser(); //logged-in user if (user != null)
			 * message.setId(user);
			 * 
			 * logger.info(message.toString());
			 */
				//invokeJMS(message);
			/*
			 * } catch (JMSException jms) { //in case MQ is not configured, just log the
			 * exception and continue logger.
			 * warning("Unable to send message to JMS provider.  Continuing without notification of change in loyalty level."
			 * ); //logException(jms); Exception linked = jms.getLinkedException(); //get
			 * the nested exception from MQ //if (linked != null) logException(linked); }
			 * catch (NamingException ne) { //in case MQ is not configured, just log the
			 * exception and continue logger.
			 * warning("Unable to lookup JMS managed resources from JNDI.  Continuing without notification of change in loyalty level."
			 * ); //logException(ne); } catch (Throwable t) { //in case MQ is not
			 * configured, just log the exception and continue logger.
			 * warning("An unexpected error occurred.  Continuing without notification of change in loyalty level."
			 * ); //logException(t); }
			 */
			return loyalty;
	}	
	
}
