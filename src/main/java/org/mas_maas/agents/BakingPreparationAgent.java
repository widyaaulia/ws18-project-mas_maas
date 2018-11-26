package org.mas_maas.agents;

import java.util.Vector;

import org.mas_maas.JSONConverter;
import org.mas_maas.messages.PreparationNotification;
import org.mas_maas.messages.PreparationRequest;
import org.mas_maas.objects.Step;

import com.google.gson.Gson;

import jade.core.AID;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class BakingPreparationAgent extends BaseAgent {
    private AID [] bakingManagerAgents;

    private Vector<String> guids;
    private Vector<Integer> productQuantities;
    private String productType;
    private Vector<Step> steps;

    protected void setup() {
        super.setup();
        System.out.println(getAID().getLocalName() + " is ready.");
        this.register("BakingPreparation", "JADE-bakery");
        this.getBakingManagerAIDs();

        // Creating receive kneading requests behaviour
        addBehaviour(new ReceivePreparationRequests());
    }

    protected void takeDown() {
        System.out.println(getAID().getLocalName() + ": Terminating.");
        this.deRegister();
    }

    public void getBakingManagerAIDs() {
        /*
        Object the AID of all the dough-manager agents found
        */
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();

        sd.setType("Baking-manager");
        template.addServices(sd);
        try {
            DFAgentDescription [] result = DFService.search(this, template);
            System.out.println(getAID().getLocalName() + "Found the following Baking-manager agents:");
            bakingManagerAgents = new AID [result.length];

            for (int i = 0; i < result.length; ++i) {
                bakingManagerAgents[i] = result[i].getName();
                System.out.println(bakingManagerAgents[i].getName());
            }

        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }

    // Receiving Preparation requests behaviour
    private class ReceivePreparationRequests extends CyclicBehaviour {
        public void action() {

            MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                MessageTemplate.MatchConversationId("preparationBaking-request"));

            ACLMessage msg = myAgent.receive(mt);

            if (msg != null) {

                System.out.println(getAID().getLocalName() + " Received baking preparation request from " + msg.getSender());

                String content = msg.getContent();
                System.out.println("Preparation request contains -> " + content);

                PreparationRequest preparationRequest = JSONConverter.parsePreparationRequest(content);
                ACLMessage reply = msg.createReply();

                reply.setPerformative(ACLMessage.CONFIRM);
                reply.setContent("Preparation request was received");

                baseAgent.sendMessage(reply);

                guids = preparationRequest.getGuids();
                productType = preparationRequest.getProductType();
                steps = preparationRequest.getSteps();
                productQuantities = preparationRequest.getProductQuantities();
                addBehaviour(new Preparation());
            }
            else {
                block();
            }
        }
    }

    // performs Preparation process
    private class Preparation extends Behaviour {
        private float stepCounter = (float) 0;
        private Float stepDuration;

        public void action(){
            if (getAllowAction()){

                for (int i = 0; i < guids.size(); i++){
                    for (Step step : steps){
                        System.out.println("---------------------------");
                        System.out.println(guids.get(i) + " Performing " + step.getAction());

                        stepDuration = step.getDuration();

                        System.out.println(" Preparation for " + stepDuration);

                        while(stepCounter < stepDuration){
                            stepCounter++;
                            System.out.println("----> " + getAID().getLocalName() + " Step counter " + stepCounter);
                        }

                        stepCounter = (float) 0;
                    }
                    addBehaviour(new SendPreparationNotification(bakingManagerAgents));
                }

                this.done();
            }


        }
        public boolean done(){
            baseAgent.finished();
            return true;
        }
  }



  // Send a preparationNotification msg to the doughManager agents
  private class SendPreparationNotification extends Behaviour {
    private AID [] bakingManagerAgents;
    private MessageTemplate mt;
    private int step = 0;
    private Gson gson = new Gson();
    private PreparationNotification preparationNotification = new PreparationNotification(guids,productType);
    private String preparationNotificationString = gson.toJson(preparationNotification);

    public SendPreparationNotification(AID [] bakingManagerAgents){
        this.bakingManagerAgents = bakingManagerAgents;
    }

       public void action() {

           switch (step) {
                case 0:
                    ACLMessage msg = new ACLMessage(ACLMessage.INFORM);

                    msg.setContent(preparationNotificationString);

                    msg.setConversationId("preparationBaking-notification");

                    // Send preparationNotification msg to bakingManagerAgents
                    for (int i = 0; i < bakingManagerAgents.length; i++){
                        msg.addReceiver(bakingManagerAgents[i]);
                    }

                    msg.setReplyWith("msg" + System.currentTimeMillis());

                    baseAgent.sendMessage(msg);

                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("preparationBaking-notification"),

                    MessageTemplate.MatchInReplyTo(msg.getReplyWith()));

                    step = 1;

                    System.out.println(getAID().getLocalName() + " Sent baking preparationNotification");
                    break;

                case 1:
                    ACLMessage reply = baseAgent.receive(mt);

                    if (reply != null) {

                        if (reply.getPerformative() == ACLMessage.CONFIRM) {
                            System.out.println(getAID().getLocalName() + " Received confirmation from " + reply.getSender());
                            step = 2;
                        }
                    }
                    else {
                        block();
                    }
                    break;

                default:
                    break;
           }
       }

       public boolean done() {
           if (step == 2) {
               baseAgent.finished(); // calling finished method
               myAgent.doDelete();
               return true;
           }

           return false;
       }
   }

}
