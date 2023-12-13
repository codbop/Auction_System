package auction;

import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

import java.util.*;

public class BidderAgent extends Agent {
	
	private Random rand = new Random();
	private int basePrice;
	private static int lastBid;
	private String name;
	
	// Items owned by the agent after the auction
	private ArrayList<String> collection;
	
	// Put agent initializations here
	protected void setup() {
		name = getAID().getName().split("@")[0];
		
		// Create the collection
		collection = new ArrayList<String>();
	    
		// Register the auction service in the yellow pages
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("auction-bidding");
		sd.setName("JADE-auction");
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}
		
		// Add the behaviour serving queries from auctioner agents
		addBehaviour(new BidRequestsServer());
		
		// The agent that buys the auctioned item reports that it has purchased the item,
		// and the last highest bid is reset again.
		addBehaviour(new InformPurchaseServer());
	}
	
	// Put agent clean-up operations here
	protected void takeDown() {
		
		// Deregister from the yellow pages
		try {
			DFService.deregister(this);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}
		
		// Print the state of the collection after the auction
		System.out.println("----------***----------");
		System.out.println("Collection of " + name + "");
		System.out.println(collection);
		System.out.println("----------***----------\n");
		
		// Printout a dismissal message
		System.out.println("----------***----------");
		System.out.println("Bidder agent " + name + " terminating.");
		System.out.println("----------***----------\n");
	    try {
	        Thread.sleep(1500);
	    } catch(InterruptedException e) {
	        System.out.println("got interrupted!");
	    }
	}
	
	private class BidRequestsServer extends CyclicBehaviour {
		public void action() {
			
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
			ACLMessage msg = myAgent.receive(mt);
			
			if (msg != null) {
				
				// CFP Message received. Process it
				ACLMessage reply = msg.createReply();
				basePrice = Integer.parseInt(msg.getContent());
				
				reply.setPerformative(ACLMessage.PROPOSE);
				
				// The agent has a 50% chance of either making an offer or not making an offer.
				if (rand.nextInt(2) == 1) { 
					if (lastBid == 0) {
						lastBid = basePrice;
					}
					else {	
						
							// A random number between 0 and 100 was generated to control the probabilities of how much 
							// the bid will be increased.
							int possibility = rand.nextInt(100) + 1;
							float incrementRate = 0;
							if (1 <= possibility && possibility <= 5) {
								incrementRate = rand.nextInt(76) + 25;
							}
							else if (6 <= possibility && possibility <= 15) {
								incrementRate = rand.nextInt(21) + 5;
							}
							else {
								incrementRate = rand.nextInt(5) + 1;
							}
							lastBid += Math.round(basePrice * (incrementRate / 100));
					}
					reply.setContent(String.valueOf(lastBid));
				}
				else {
					reply.setContent("0");
				}
				myAgent.send(reply);
			}
			else {
				// If auction is closed terminate the agents.
				mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
				msg = myAgent.receive(mt);

				if (msg != null) {
					String content = msg.getContent();
					if (content.equals("Auction has closed")) {
						doDelete();
					}
				}	
				block();
			}
		}
	}
	
	private class InformPurchaseServer extends CyclicBehaviour {
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null ) {
				
				// ACCEPT_PROPOSAL Message received. Process it
				String item = msg.getContent();
				collection.add(item);
				
				// Last bid is reset to 0
				lastBid = 0;
				
    			System.out.println("----------***----------");
				System.out.println(name + ": I am the new owner of " + item);
    			System.out.println("----------***----------\n");
			}
		}
	}
}