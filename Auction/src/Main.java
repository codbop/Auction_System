import jade.core.AgentContainer;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;


public class Main {
	
	public static void main(String[] args) {
		
		//Get the JADE runtime interface (singleton)
		Runtime rt = Runtime.instance();
		
		//Create a Profile, where the launch arguments are stored
		Profile p = new ProfileImpl();
		p.setParameter(Profile.CONTAINER_NAME, "Auction");
		p.setParameter(Profile.MAIN_HOST, "localhost");
		p.setParameter(Profile.GUI, "true");
		
		// Create container
		ContainerController cc = rt.createMainContainer(p);
		try {
			AgentController bidder_1 = cc.createNewAgent("BidderAgent_1", "auction.BidderAgent", null);//arguments
			bidder_1.start();
			AgentController bidder_2 = cc.createNewAgent("BidderAgent_2", "auction.BidderAgent", null);//arguments
			bidder_2.start();
			AgentController bidder_3 = cc.createNewAgent("BidderAgent_3", "auction.BidderAgent", null);//arguments
			bidder_3.start();
			AgentController auctioner = cc.createNewAgent("AuctionerAgent", "auction.AuctionerAgent", null);
			auctioner.start();
		} catch (StaleProxyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		//}
		}
	}
}
