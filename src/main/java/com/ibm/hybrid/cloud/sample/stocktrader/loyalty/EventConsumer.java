package com.ibm.hybrid.cloud.sample.stocktrader.loyalty;

import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;

import com.ibm.hybrid.cloud.sample.stocktrader.odm.ODMHelper;
import com.ibm.hybrid.cloud.sample.stocktrader.portfolio.event.BaseEvent;
import com.ibm.hybrid.cloud.sample.stocktrader.portfolio.event.LoyaltyChangeEvent;
import com.ibm.hybrid.cloud.sample.stocktrader.portfolio.event.StockPurchasedEvent;

@ApplicationScoped
public class EventConsumer {
	private static Logger logger = Logger.getLogger(EventConsumer.class.getName());
	private Jsonb jsonb = JsonbBuilder.create();
	
	@Inject
	private ODMHelper odmHelper;

	@Incoming("stock-channel-inbound")
	@Outgoing("stock-channel-outbound")
	public PublisherBuilder<String> receive(String eventAsString) {
		logger.info("Received: " + eventAsString);
		BaseEvent event = null;
		try {
			event = jsonb.fromJson(eventAsString, BaseEvent.class);
		}
		catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		String type = event.getType();
		
		if(type.equals(BaseEvent.TYPE_PURCHASE)) {
			// handle 
			StockPurchasedEvent stockPurchasedEvent = jsonb.fromJson(eventAsString, StockPurchasedEvent.class);
			double overallTotal = stockPurchasedEvent.getOverallTotal();
			logger.info("overallTotal: " + overallTotal);
			// invoke ODM
			
			String newLoyalty = stockPurchasedEvent.getLoyalty();
					
			try {
				newLoyalty = odmHelper.invokeODM(stockPurchasedEvent.getOwner(), overallTotal, stockPurchasedEvent.getLoyalty());
			}
			catch (Throwable e) {
				logger.severe("Cannot contact odm: " + e.getMessage());
				e.printStackTrace();
				event.setType(BaseEvent.TYPE_LOYALTY_CHECK_FAILED);
				return ReactiveStreams.of(jsonb.toJson(event));
			}
			logger.info("oldLoyalty:" + stockPurchasedEvent.getLoyalty() + " newLoyalty: " +  newLoyalty);
			if(!stockPurchasedEvent.getLoyalty().equals(newLoyalty)) {
				LoyaltyChangeEvent loyaltyEvent = new LoyaltyChangeEvent(event);
				loyaltyEvent.setType(BaseEvent.TYPE_LOYALTY_CHANGED);
				loyaltyEvent.setOldLoyalty(stockPurchasedEvent.getLoyalty());
				loyaltyEvent.setNewLoyalty(newLoyalty);
				return ReactiveStreams.of(jsonb.toJson(loyaltyEvent));
			}
			else {
				LoyaltyChangeEvent loyaltyEvent = new LoyaltyChangeEvent(event);
				loyaltyEvent.setType(BaseEvent.TYPE_LOYALTY_NOT_CHANGED);
				return ReactiveStreams.of(jsonb.toJson(loyaltyEvent));
			}
			
			// create event

		}
		else {
			logger.info("Not interested in event: " + type);
			return ReactiveStreams.empty();
		}
	}
}
