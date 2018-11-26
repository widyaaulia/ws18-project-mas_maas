package org.mas_maas.agents;

import java.util.Vector;
import java.util.Scanner;
import java.io.File;
import java.io.FileNotFoundException;

import org.mas_maas.JSONConverter;
import org.mas_maas.messages.PreparationNotification;
import org.mas_maas.messages.PreparationRequest;

import org.mas_maas.objects.Step;
import org.mas_maas.objects.Bakery;
import org.mas_maas.objects.DoughPrepTable;
import org.mas_maas.objects.Equipment;

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

public class PreparationTableAgent extends BaseAgent {
    private AID [] doughManagerAgents;

    private Bakery bakery;
    private Vector<Equipment> preparationTables = new Vector<Equipment> ();
    private Vector<Boolean> tablesAvailable = new Vector<Boolean> ();
    private Vector<Equipment> equipment;

    private Vector<String> guids;
    private Vector<Integer> productQuantities;
    private String productType;
    private Vector<Step> steps;

    protected void setup() {
        super.setup();

        System.out.println(getAID().getLocalName() + " is ready.");

        this.register("Preparation-table", "JADE-bakery");

        this.getDoughManagerAIDs();

        // Load bakery information (includes recipes for each product)
        getbakery();

        // Get KneadingMachines
        this.getPreparationTables();


        // Creating receive kneading requests behaviour
        addBehaviour(new ReceivePreparationRequests());
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

    public void getbakery(){

        String jsonDir = "src/main/resources/config/shared_stage_communication/";
        try {
            // System.out.println("Working Directory = " + System.getProperty("user.dir"));
            String bakeryFile = new Scanner(new File(jsonDir + "bakery.json")).useDelimiter("\\Z").next();
            Vector<Bakery> bakeries = JSONConverter.parseBakeries(bakeryFile);
            for (Bakery bakery : bakeries)
            {
                this.bakery = bakery;
            }
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void getPreparationTables(){
        equipment = bakery.getEquipment();
        System.out.println("==============================================");
        System.out.println("Bakery name " + bakery.getName());

        for (int i = 0; i < equipment.size(); i++){
            if (equipment.get(i) instanceof DoughPrepTable){
                preparationTables.add(equipment.get(i));
                tablesAvailable.add(true);
            }
        }

        System.out.println("Preparation tables found " + preparationTables.size());
        System.out.println("Preparation tables flags " + tablesAvailable);
        System.out.println("==============================================");

    }

    // Receiving Preparation requests behaviour
    private class ReceivePreparationRequests extends CyclicBehaviour {
        public void action() {

            MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                MessageTemplate.MatchConversationId("preparation-request"));

            ACLMessage msg = myAgent.receive(mt);

            if (msg != null) {

                System.out.println(getAID().getLocalName() + " Received preparation request from " + msg.getSender());

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
        private int tableAvailable;

        public int findAvailableTables(){
            for (int i = 0; i < preparationTables.size(); i++){
                if (tablesAvailable.get(i) == true){
                    return i;
                }
            }
            return (int) 99;
        }

        public void action(){
            tableAvailable = this.findAvailableTables();

            if (getAllowAction() && tableAvailable != 99){
                tablesAvailable.set(tableAvailable,false);
                System.out.println("----> Using preparation table " + preparationTables.get(tableAvailable));

                for (int i = 0; i < guids.size(); i++){
                    for (Step step : steps){
                        System.out.println("---------------------------");
                        System.out.println(guids.get(i) + " Performing " + step.getAction());

                        if (step.getAction().equals(Step.ITEM_PREPARATION_STEP)){
                            stepDuration = step.getDuration() * productQuantities.get(i);
                        }else{
                            stepDuration = step.getDuration();
                        }

                        System.out.println(" Preparation for " + stepDuration);

                        while(stepCounter < stepDuration){
                            stepCounter++;
                            System.out.println("----> " + getAID().getLocalName() + " Step counter " + stepCounter);
                        }

                        stepCounter = (float) 0;
                    }
                    tablesAvailable.set(tableAvailable,true);
                    addBehaviour(new SendPreparationNotification(doughManagerAgents));
                }

                this.done();
            } else{
                System.out.println("----> No preparation table currently available");
            }


        }
        public boolean done(){
            baseAgent.finished();
            return true;
        }
  }



  // Send a preparationNotification msg to the doughManager agents
  private class SendPreparationNotification extends Behaviour {
    private AID [] doughManagerAgents;
    private MessageTemplate mt;
    private int step = 0;
    private Gson gson = new Gson();
    private PreparationNotification preparationNotification = new PreparationNotification(guids,productType);
    private String preparationNotificationString = gson.toJson(preparationNotification);

    public SendPreparationNotification(AID [] doughManagerAgents){
        this.doughManagerAgents = doughManagerAgents;
    }

       public void action() {

           switch (step) {
                case 0:
                    ACLMessage msg = new ACLMessage(ACLMessage.INFORM);

                    msg.setContent(preparationNotificationString);

                    msg.setConversationId("preparation-notification");

                    // Send preparationNotification msg to doughManagerAgents
                    for (int i = 0; i < doughManagerAgents.length; i++){
                        msg.addReceiver(doughManagerAgents[i]);
                    }

                    msg.setReplyWith("msg" + System.currentTimeMillis());

                    baseAgent.sendMessage(msg);

                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("preparation-notification"),

                    MessageTemplate.MatchInReplyTo(msg.getReplyWith()));

                    System.out.println(getAID().getLocalName() + " Sent preparationNotification");

                    step = 1;

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
