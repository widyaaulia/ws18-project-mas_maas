package org.mas_maas.agents;

import java.util.Vector;

import org.mas_maas.JSONConverter;
import org.mas_maas.messages.KneadingNotification;
import org.mas_maas.messages.KneadingRequest;

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

public class KneadingMachineAgent extends BaseAgent {
    private AID [] doughManagerAgents;

    private Vector<String> guids;
    private String productType;

    protected void setup() {
        super.setup();

        System.out.println(getAID().getLocalName() + " is ready.");

        // Register KneadingMachine Agent to the yellow Pages
        this.register("Kneading-machine", "JADE-bakery");

        // Get Agents AIDS
        this.getDoughManagerAIDs();

        // Creating receive kneading requests behaviour
        addBehaviour(new ReceiveKneadingRequests());

    }

    protected void takeDown() {
        System.out.println(getAID().getLocalName() + ": Terminating.");
        this.deRegister();
    }

    public void getDoughManagerAIDs() {
        /*
        Object the AID of all the dough-manager agents found
        */
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();

        sd.setType("Dough-manager");
        template.addServices(sd);
        try {
            DFAgentDescription [] result = DFService.search(this, template);
            System.out.println(getAID().getLocalName() + "Found the following Dough-manager agents:");
            doughManagerAgents = new AID [result.length];

            for (int i = 0; i < result.length; ++i) {
                doughManagerAgents[i] = result[i].getName();
                System.out.println(doughManagerAgents[i].getName());
            }

        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }

    // Receiving Kneading requests behavior
    private class ReceiveKneadingRequests extends CyclicBehaviour {
        public void action() {

            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);

            ACLMessage msg = myAgent.receive(mt);

            if (msg != null) {

                System.out.println(getAID().getLocalName() + " received requests.");
                String content = msg.getContent();
                KneadingRequest kneadingRequest = JSONConverter.parseKneadingRequest(content);
                ACLMessage reply = msg.createReply();

                reply.setPerformative(ACLMessage.CONFIRM);
                reply.setContent("Kneading request was received");
                baseAgent.sendMessage(reply);
                Float kneadingTime = kneadingRequest.getKneadingTime();
                guids = kneadingRequest.getGuids();
                productType = kneadingRequest.getProductType();

                addBehaviour(new Kneading(kneadingTime));

            }

            else {
                block();
            }
        }
    }

    // performs Kneading process
    private class Kneading extends Behaviour {
        private Float kneadingTime;
        private Float kneadingCounter = (float) 0;
        private int option = 0;

        public Kneading(Float kneadingTime){
            this.kneadingTime = kneadingTime;
            System.out.println(getAID().getLocalName() + " Kneading for " + kneadingTime);
        }

        public void action(){

            switch(option){

                case 0:
                    if (getAllowAction() == true){
                        kneadingCounter = kneadingTime;

                        if  (kneadingCounter == kneadingTime){
                            System.out.println("============================");
                            System.out.println("Kneading completed");
                            System.out.println("============================");
                            option = 1;

                             // Creating send kneading notification behavior
                             addBehaviour(new SendKneadingNotification(doughManagerAgents));

                        }else{
                            System.out.println("============================");
                            System.out.println("Kneading in process...");
                            System.out.println("============================");
                        }

                        baseAgent.finished();
                    }
            }

        }
        public boolean done(){
            if (option == 1){
                return true;

            }
            else{
                return false;
            }
      }
    }

    // Send a kneadingNotification msg to the doughManager agents
    private class SendKneadingNotification extends Behaviour {
        private AID [] doughManagerAgents;
        private MessageTemplate mt;
        private int option = 0;

        Gson gson = new Gson();

        KneadingNotification kneadingNotification = new KneadingNotification(guids,productType);

        String kneadingNotificationString = gson.toJson(kneadingNotification);

        public SendKneadingNotification(AID [] doughManagerAgents){

            this.doughManagerAgents = doughManagerAgents;
        }

        public void action() {
            switch (option) {
                case 0:

                    ACLMessage msg = new ACLMessage(ACLMessage.INFORM);

                    msg.setContent(kneadingNotificationString);

                    msg.setConversationId("kneading-notification");

                    // Send kneadingNotification msg to doughManagerAgents
                    for (int i = 0; i < doughManagerAgents.length; i++){
                        msg.addReceiver(doughManagerAgents[i]);
                    }

                    msg.setReplyWith("msg" + System.currentTimeMillis());

                    baseAgent.sendMessage(msg);

                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("kneading-notification"),

                    MessageTemplate.MatchInReplyTo(msg.getReplyWith()));

                    option = 1;

                    System.out.println(getAID().getLocalName() + " Sent kneadingNotification");

                    break;

                case 1:
                    ACLMessage reply = baseAgent.receive(mt);

                    if (reply != null) {

                        if (reply.getPerformative() == ACLMessage.CONFIRM) {
                            System.out.println(getAID().getLocalName() + " Received confirmation");
                            option = 2;
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
            if (option == 2) {
                baseAgent.finished();
                myAgent.doDelete();
                return true;
            }

           return false;
       }
    }

}
