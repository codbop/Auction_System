package auction;
import jade.core.Agent;

import javax.swing.JOptionPane;

import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

import java.util.*;

public class AuctionerAgent extends Agent {
	private Random rand = new Random();
	
	private AID[] bidderAgents;
	
	// The catalogue of items for bidding
	private ArrayList<String[]> catalogue;
	
	private String basePrice;
	private String currentAuctionItem;
	private int bidCountLimit;
	private String name;
	
	// Controls if auction is in process
	private Boolean lock = false;
	   
	protected void setup() {
		
		name = getAID().getName().split("@")[0];
		
		System.out.println("----------***----------");
	    System.out.println("Hallo! Auctioner-agent "+name+" is ready.");
		System.out.println("----------***----------\n");
	    try {
	        Thread.sleep(1500);
	    } catch(InterruptedException e) {
	        System.out.println("got interrupted!");
	    }
	    
	    // Create the catalogue
	    catalogue = new ArrayList<String[]>();
	    catalogue.add(new String[]{"item_1", String.valueOf(rand.nextInt(10000))});
	    catalogue.add(new String[]{"item_2", String.valueOf(rand.nextInt(10000))});
	    catalogue.add(new String[]{"item_3", String.valueOf(rand.nextInt(10000))});
	    catalogue.add(new String[]{"item_4", String.valueOf(rand.nextInt(10000))});
	    catalogue.add(new String[]{"item_5", String.valueOf(rand.nextInt(10000))});
	    
		// Add a TickerBehaviour that schedules a request to bidder agents every minute
	    addBehaviour(new CyclicBehaviour() {
	    	public void action() {
	    		
	    		if (!lock) {
	    			DFAgentDescription template = new DFAgentDescription();
	    			ServiceDescription sd = new ServiceDescription();
	    			sd.setType("auction-bidding");
	    			template.addServices(sd);
	
		    		
		    		// Update the list of bidder agents
		    		try {
		    			DFAgentDescription[] result = DFService.search(myAgent, template);
		    			System.out.println("Found the following bidder agents:");
		    			bidderAgents = new AID[result.length];
		    			
		    			System.out.println("----------***----------");
		    			for (int i = 0; i < result.length; ++i) {
		    				bidderAgents[i] = result[i].getName();
		    				System.out.println(bidderAgents[i].getName().split("@")[0]);
		    			    try {
		    			        Thread.sleep(1000);
		    			    } catch(InterruptedException e) {
		    			        System.out.println("got interrupted!");
		    			    }
		    			}
		    			System.out.println("----------***----------\n");
		    		}
		    		catch (FIPAException fe) {
		    			fe.printStackTrace();
		    		}
		    		
		    		if (catalogue.size() != 0) {
			    		
			    		// The next item is put up for auction
		    			currentAuctionItem = catalogue.get(0)[0];
			    		basePrice = catalogue.get(0)[1];
			    		
			    		// The maximum number of bids for an item to be auctioned off is 20 
			    		// and the minimum number of bids is 5.
			    		bidCountLimit = rand.nextInt(15) + 5;
			    		
		    			System.out.println("----------***----------");
			    		System.out.println(name + ": Bids are being taken for " + currentAuctionItem +
			    				"\nBase price is $" + basePrice + ".");
			    	    try {
			    	        Thread.sleep(1500);
			    	    } catch(InterruptedException e) {
			    	        System.out.println("got interrupted!");
			    	    }
		    			System.out.println("----------***----------\n");
			    		
			    		// Perform the request
			    		myAgent.addBehaviour(new RequestPerformer());
			    		
			    		lock = true;
			    		
		    		}
		    		else if (catalogue.size() == 0) {
		    			
		    			// Make the agent terminate
		    			System.out.println("----------***----------");
		    			System.out.println(name + ": There are not items left to be auctioned.");
		    			System.out.println(name + ": Auction has closed.");
		    			System.out.println("----------***----------\n");
		    			
		    			// Notify bidder agents that the action has closed.
						ACLMessage inform = new ACLMessage(ACLMessage.INFORM);
						for (int i = 0; i < bidderAgents.length; i++) {
							inform.addReceiver(bidderAgents[i]);
						}
						inform.setContent("Auction has closed");
						inform.setConversationId("auction");
						inform.setReplyWith("inform" + System.currentTimeMillis()); // Unique value
			    	    try {
			    	        Thread.sleep(1500);
			    	    } catch(InterruptedException e) {
			    	        System.out.println("got interrupted!");
			    	    }
						
						myAgent.send(inform);
		    			
			    	    try {
			    	        Thread.sleep(1500);
			    	    } catch(InterruptedException e) {
			    	        System.out.println("got interrupted!");
			    	    }
		    			doDelete();
		    		}
	    		}
	    		else {
	    			block();
	    		}
	    	}
	    } );
	}
	
	// Put agent clean-up operations here
	protected void takeDown() {
		
		// Printout a dismissal message
		System.out.println("----------***----------");
		System.out.println("Auctioner-agent " + getAID().getName() + " terminating.");
		System.out.println("----------***----------\n");
	    try {
	        Thread.sleep(1500);
	    } catch(InterruptedException e) {
	        System.out.println("got interrupted!");
	    }
	}
	
	private class RequestPerformer extends Behaviour {
		private AID bestBidder; // The agent who provides best bid
		private int bestPrice; // The best offered price
		private int bidCount; // The counter of bid replies from bidder agents
		private int replyCount = 0; // Number of replies made in 1 round
		private MessageTemplate mt;
		private int step;
		public void action() {
						
			switch (step) {
				case 0:
					
					// Send the cfp to all bidders
					ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
					for (int i = 0; i < bidderAgents.length; i++) {
						cfp.addReceiver(bidderAgents[i]);
					}
					cfp.setContent(basePrice);
					cfp.setConversationId("auction");
					cfp.setReplyWith("cfp" + System.currentTimeMillis()); // Unique value
					
					myAgent.send(cfp);
					
					// Prepare the template to get bids
					mt = MessageTemplate.and(MessageTemplate.MatchConversationId("auction"),
							MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
					step = 1;
					break;
				case 1:
					
					// Receive all bids from bidder agents
					ACLMessage reply = myAgent.receive(mt);
					
					if (reply != null) {
						
						// Reply received
						if (reply.getPerformative() == ACLMessage.PROPOSE) {
							
							int price = Integer.parseInt(reply.getContent());
							
							// If price is 0 this means agent has not made a bid
							if (price != 0) {
								
								// This is the best bid at present
								bestPrice = price;
								bestBidder = reply.getSender();
								
				    			System.out.println("----------***----------");
					    		System.out.println(reply.getSender().getName().split("@")[0] + ": " + "$" + bestPrice);
				    			System.out.println("----------***----------\n");
					    	    try {
					    	        Thread.sleep(1500);
					    	    } catch(InterruptedException e) {
					    	        System.out.println("got interrupted!");
					    	    }
							}
							else {
				    			System.out.println("----------***----------");
					    		System.out.println(reply.getSender().getName().split("@")[0] + ": " + "No bid");
				    			System.out.println("----------***----------\n");
					    	    try {
					    	        Thread.sleep(1500);
					    	    } catch(InterruptedException e) {
					    	        System.out.println("got interrupted!");
					    	    }
							}
						}
						bidCount++;
						replyCount++;
						if (bidCount >= bidCountLimit) {
							
							// Enough offers received to sold the item
							step = 2;
						}
						else if (replyCount >= bidderAgents.length) {
							
							// Number of bids for 1 round reached. Replies were received from
							// all agents. Ask agents to bid again.
							step = 0;
						}
					}
					break;
				case 2:
					catalogue.remove(0);
					
					// If at least one bid was taken notify the best bidder that the sale is complete 
					if (bestBidder != null) {
						ACLMessage sale = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
						sale.addReceiver(bestBidder);
						sale.setContent(currentAuctionItem);
						sale.setConversationId("auction");
						sale.setReplyWith("sale" + System.currentTimeMillis());
						myAgent.send(sale);
						
		    			System.out.println("----------***----------");
						System.out.println(name + ": " + currentAuctionItem + " sold to agent " + 
		    			bestBidder.getName().split("@")[0] + " for $" + bestPrice + ".");
		    			System.out.println("----------***----------\n");
			    	    try {
			    	        Thread.sleep(3000);
			    	    } catch(InterruptedException e) {
			    	        System.out.println("got interrupted!");
			    	    }
					}
		    		else {
		    			System.out.println("----------***----------");
						System.out.println(name + ": No bids were taken so item was not sold.");
		    			System.out.println("----------***----------\n");
			    	    try {
			    	        Thread.sleep(1500);
			    	    } catch(InterruptedException e) {
			    	        System.out.println("got interrupted!");
			    	    }
		    		}
					
					step = 3;
					break;
			}
		}
		
		public boolean done() {
			if (step == 3) {
				lock = false;
			}
			return (step == 3);
		}		
	}
}