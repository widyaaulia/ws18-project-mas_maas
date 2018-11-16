package org.mas_maas;

import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.Vector;

import org.mas_maas.messages.DoughNotification;
import org.mas_maas.messages.KneadingNotification;
import org.mas_maas.messages.KneadingRequest;
import org.mas_maas.messages.PreparationNotification;
import org.mas_maas.messages.PreparationRequest;
import org.mas_maas.messages.ProofingRequest;
import org.mas_maas.objects.BakedGood;
import org.mas_maas.objects.Bakery;
import org.mas_maas.objects.Batch;
import org.mas_maas.objects.Client;
import org.mas_maas.objects.DeliveryCompany;
import org.mas_maas.objects.DoughPrepTable;
import org.mas_maas.objects.Equipment;
import org.mas_maas.objects.KneadingMachine;
import org.mas_maas.objects.MetaInfo;
import org.mas_maas.objects.Order;
import org.mas_maas.objects.Oven;
import org.mas_maas.objects.Packaging;
import org.mas_maas.objects.Product;
import org.mas_maas.objects.Recipe;
import org.mas_maas.objects.Step;
import org.mas_maas.objects.StreetLink;
import org.mas_maas.objects.StreetNetwork;
import org.mas_maas.objects.StreetNode;
import org.mas_maas.objects.Truck;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class JSONConverter
{
    public static void test_parsing()
    {
        String sampleDir = "src/main/resources/config/sample/";
        String doughDir = "src/main/resources/config/dough_stage_communication/";
        try {
            //System.out.println("Working Directory = " + System.getProperty("user.dir"));

            String bakeryFile = new Scanner(new File(sampleDir + "bakeries.json")).useDelimiter("\\Z").next();
            Vector<Bakery> bakeries = parseBakeries(bakeryFile);
            for (Bakery bakery : bakeries)
            {
                System.out.println(bakery);
            }

            String clientFile = new Scanner(new File(sampleDir + "clients.json")).useDelimiter("\\Z").next();
            Vector<Client> clients = parseClients(clientFile);
            for (Client client : clients)
            {
                System.out.println(client);
            }

            String deliveryCompanyFile = new Scanner(new File(sampleDir + "delivery.json")).useDelimiter("\\Z").next();
            Vector<DeliveryCompany> deliveryCompanies = parseDeliveryCompany(deliveryCompanyFile);
            for (DeliveryCompany deliveryCompany : deliveryCompanies)
            {
                System.out.println(deliveryCompany);
            }

            String metaInfoFile = new Scanner(new File(sampleDir + "meta.json")).useDelimiter("\\Z").next();
            MetaInfo metaInfo = parseMetaInfo(metaInfoFile);
            System.out.println(metaInfo);

            String streetNetworkFile = new Scanner(new File(sampleDir + "street-network.json")).useDelimiter("\\Z").next();
            StreetNetwork streetNetwork = parseStreetNetwork(streetNetworkFile);
            System.out.println(streetNetwork);

            String doughNotificationString = new Scanner(new File(doughDir + "dough_notification.json")).useDelimiter("\\Z").next();
            DoughNotification doughtNotification = parseDoughNotification(doughNotificationString);
            System.out.println(doughtNotification);

            String kneadingNotificationString = new Scanner(new File(doughDir + "kneading_notification.json")).useDelimiter("\\Z").next();
            KneadingNotification kneadingNotification = parseKneadingNotification(kneadingNotificationString);
            System.out.println(kneadingNotification);

            String kneadingRequestString = new Scanner(new File(doughDir + "kneading_request.json")).useDelimiter("\\Z").next();
            KneadingRequest kneadingRequest = parseKneadingRequest(kneadingRequestString);
            System.out.println(kneadingRequest);

            String preparationNotificationString = new Scanner(new File(doughDir + "preparation_notification.json")).useDelimiter("\\Z").next();
            PreparationNotification preparationNotification = parsePreparationNotification(preparationNotificationString);
            System.out.println(preparationNotification);

            String preparationRequestString = new Scanner(new File(doughDir + "preparation_request.json")).useDelimiter("\\Z").next();
            PreparationRequest preparationRequest = parsePreparationRequest(preparationRequestString);
            System.out.println(preparationRequest);

            String proofingRequestString = new Scanner(new File(doughDir + "proofing_request.json")).useDelimiter("\\Z").next();
            ProofingRequest proofingRequest = parseProofingRequest(proofingRequestString);
            System.out.println(proofingRequest);



        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static Vector<Bakery> parseBakeries(String jsonFile)
    {
        JsonElement root = new JsonParser().parse(jsonFile);
        JsonArray arr = root.getAsJsonArray();

        Vector<Bakery> bakeries = new Vector<Bakery>();
        for (JsonElement element : arr)
        {
            // bakery
            JsonObject json_bakery = element.getAsJsonObject();
            String guid = json_bakery.get("guid").getAsString();
            String name = json_bakery.get("name").getAsString();
            JsonObject json_location = (JsonObject) json_bakery.get("location");
            Double x = json_location.get("x").getAsDouble();
            Double y = json_location.get("y").getAsDouble();
            Point2D location = new Point2D.Double(x, y);

            // products
            Vector<Product> products = new Vector<Product>();
            JsonArray json_products = json_bakery.get("products").getAsJsonArray();
            for (JsonElement product : json_products)
            {
                JsonObject json_product = product.getAsJsonObject();
                String product_guid = json_product.get("guid").getAsString();

                JsonObject json_batch = json_product.get("batch").getAsJsonObject();
                int breadsPerOven = json_batch.get("breadsPerOven").getAsInt();
                Batch batch = new Batch(breadsPerOven);

                JsonObject json_recipe = json_product.get("recipe").getAsJsonObject();
                int coolingRate = json_recipe.get("coolingRate").getAsInt();
                int bakingTemp = json_recipe.get("bakingTemp").getAsInt();

                JsonArray step_array = json_recipe.get("steps").getAsJsonArray();
                Vector<Step> steps = new Vector<Step>();
                for (JsonElement step : step_array)
                {
                    JsonObject json_step = step.getAsJsonObject();
                    String action = json_step.get("action").getAsString();
                    Float duration = json_step.get("duration").getAsFloat();
                    Step aStep = new Step(action, duration);
                    steps.add(aStep);
                }
                Recipe recipe = new Recipe(coolingRate, bakingTemp, steps);

                JsonObject json_packaging = json_product.get("packaging").getAsJsonObject();
                int boxingTemp = json_packaging.get("boxingTemp").getAsInt();
                int breadsPerBox = json_packaging.get("breadsPerBox").getAsInt();
                Packaging packaging = new Packaging(boxingTemp, breadsPerBox);

                Double salesPrice = json_product.get("salesPrice").getAsDouble();
                Double productionCost = json_product.get("productionCost").getAsDouble();

                Product aProduct = new Product(product_guid, batch, recipe, packaging, salesPrice,  productionCost);
                products.add(aProduct);
            }


            // equipment
            Vector<Equipment> equipment = new Vector<Equipment>();
            JsonObject json_equipment = json_bakery.get("equipment").getAsJsonObject();
            JsonArray ovens = json_equipment.get("ovens").getAsJsonArray();
            for (JsonElement oven : ovens)
            {
                JsonObject json_oven = oven.getAsJsonObject();
                String oven_guid = json_oven.get("guid").getAsString();

                // multiple ways of denoting rates (CamelCase and _ are used)
                // TODO cleanup and make into a function
                int coolingRate = -1;
                if (json_oven.toString().contains("coolingRate"))
                {
                    coolingRate = json_oven.get("coolingRate").getAsInt();
                }
                else if (json_oven.toString().contains("cooling_rate"))
                {
                    coolingRate = json_oven.get("cooling_rate").getAsInt();
                }

                // TODO cleanup and make into a function
                int heatingRate = -1;
                if (json_oven.toString().contains("heatingRate"))
                {
                    heatingRate = json_oven.get("heatingRate").getAsInt();
                }
                else if (json_oven.toString().contains("heating_rate"))
                {
                    heatingRate = json_oven.get("heating_rate").getAsInt();
                }

                Oven anOven = new Oven(oven_guid, coolingRate, heatingRate);
                equipment.add(anOven);
            }

            JsonArray doughPrepTables = json_equipment.get("doughPrepTables").getAsJsonArray();
            for (JsonElement table : doughPrepTables)
            {
                JsonObject json_table = table.getAsJsonObject();
                String table_guid = json_table.get("guid").getAsString();

                DoughPrepTable aTable = new DoughPrepTable(table_guid);
                equipment.add(aTable);
            }

            JsonArray kneadingMachines = json_equipment.get("kneadingMachines").getAsJsonArray();
            for (JsonElement kneadingMachine : kneadingMachines)
            {
                JsonObject json_kneadingMachine = kneadingMachine.getAsJsonObject();
                String kneadingMachine_guid = json_kneadingMachine.get("guid").getAsString();

                KneadingMachine aMachine = new KneadingMachine(kneadingMachine_guid);
                equipment.add(aMachine);
            }

            Bakery bakery = new Bakery(guid, name, location, products, equipment);
            bakeries.add(bakery);
        }

        return bakeries;
    }

    public static Vector<Client> parseClients(String jsonFile)
    {
        JsonElement root = new JsonParser().parse(jsonFile);
        JsonArray arr = root.getAsJsonArray();

        Vector<Client> clients = new Vector<Client>();
        for (JsonElement element : arr)
        {
            JsonObject json_client = element.getAsJsonObject();
            String guid = json_client.get("guid").getAsString();
            int type = json_client.get("type").getAsInt();
            String name = json_client.get("name").getAsString();
            JsonObject json_location = (JsonObject) json_client.get("location");
            Double x = json_location.get("x").getAsDouble();
            Double y = json_location.get("y").getAsDouble();
            Point2D location = new Point2D.Double(x, y);

            // orders
            Vector<Order> orders = new Vector<Order>();
            JsonArray json_orders = json_client.get("orders").getAsJsonArray();
            for (JsonElement order : json_orders)
            {
                String json_order = order.toString();
                orders.add(JSONConverter.parseOrder(json_order));
            }

            Client aClient = new Client(guid, type, name, location, orders);
            clients.add(aClient);
        }

        return clients;
    }

    public static Order parseOrder(String jsonFile)
    {
        JsonElement root = new JsonParser().parse(jsonFile);
        JsonObject json_order = root.getAsJsonObject();

        String customerId = json_order.get("customerId").getAsString();
        String order_guid = json_order.get("guid").getAsString();
        JsonObject json_orderDate = json_order.get("orderDate").getAsJsonObject();
        int orderDay = json_orderDate.get("day").getAsInt();
        int orderHour = json_orderDate.get("day").getAsInt();
        JsonObject json_deliveryDate = json_order.get("deliveryDate").getAsJsonObject();
        int deliveryDay = json_deliveryDate.get("day").getAsInt();
        int deliveryHour = json_deliveryDate.get("day").getAsInt();

        // products (BakedGood objects)
        // TODO shouldn't products be an array not an object?
        // TODO this will need to be reworked in the future when BakedGood is more fleshed out
        // TODO also this JUST is a bit hacky...
        Vector<BakedGood> bakedGoods = new Vector<BakedGood>();
        JsonObject json_products = json_order.get("products").getAsJsonObject();
        for (String bakedGoodName : BakedGood.bakedGoodNames)
        {
            int amount = json_products.get(bakedGoodName).getAsInt();
            bakedGoods.add(new BakedGood(bakedGoodName, amount));
        }

        Order anOrder = new Order(customerId, order_guid, orderDay, orderHour, deliveryDay, deliveryHour, bakedGoods);
        return anOrder;
    }

    public static Vector<DeliveryCompany> parseDeliveryCompany(String jsonFile)
    {
        JsonElement root = new JsonParser().parse(jsonFile);
        JsonArray arr = root.getAsJsonArray();

        Vector<DeliveryCompany> companies = new Vector<DeliveryCompany>();
        for (JsonElement element : arr)
        {
            JsonObject json_deliveryCompany = element.getAsJsonObject();
            String guid = json_deliveryCompany.get("guid").getAsString();
            JsonObject json_location = (JsonObject) json_deliveryCompany.get("location");
            Double x = json_location.get("x").getAsDouble();
            Double y = json_location.get("y").getAsDouble();
            Point2D location = new Point2D.Double(x, y);

            Vector<Truck> trucks = new Vector<Truck>();
            JsonArray json_trucks = json_deliveryCompany.get("trucks").getAsJsonArray();
            for (JsonElement truck : json_trucks)
            {
                JsonObject json_truck = truck.getAsJsonObject();
                String truck_guid = json_truck.get("guid").getAsString();
                int loadCapacity = json_truck.get("load_capacity").getAsInt();
                JsonObject json_truck_location = (JsonObject) json_deliveryCompany.get("location");
                Double truck_x = json_truck_location.get("x").getAsDouble();
                Double truck_y = json_truck_location.get("y").getAsDouble();
                Point2D truck_location = new Point2D.Double(truck_x, truck_y);


                Truck aTruck = new Truck(truck_guid, loadCapacity, truck_location);
                trucks.add(aTruck);
            }

            DeliveryCompany aCompany = new DeliveryCompany(guid, location, trucks);
            companies.add(aCompany);
        }

        return companies;
    }

    public static MetaInfo parseMetaInfo(String jsonFile)
    {
        JsonElement root = new JsonParser().parse(jsonFile);

        JsonObject json_metaInfo = root.getAsJsonObject();
        int bakeries = json_metaInfo.get("bakeries").getAsInt();
        int durationInDays = json_metaInfo.get("durationInDays").getAsInt();
        int products = json_metaInfo.get("products").getAsInt();
        int orders = json_metaInfo.get("orders").getAsInt();
        Vector<Client> customers = new Vector<Client>(); // TODO this will change when the json changes

        // TODO shouldn't the customers be in an array not an Object?
        Set<Entry<String, JsonElement>> entrySet = json_metaInfo.get("customers").getAsJsonObject().entrySet();
        for(Map.Entry<String,JsonElement> entry : entrySet)
        {
            Client customer = new Client();
            customer.setGuid(entry.getKey());
            // TODO fix, this is super hacky
            customer.setType(entry.getValue().getAsInt());
            customers.add(customer);
        }

        MetaInfo metaInfo = new MetaInfo(bakeries, customers, durationInDays, products, orders);

        return metaInfo;
    }

    public static StreetNetwork parseStreetNetwork(String jsonFile)
    {
        JsonElement root = new JsonParser().parse(jsonFile);
        JsonObject json_streetNetwork = root.getAsJsonObject();
        boolean directed = json_streetNetwork.get("directed").getAsBoolean();

        Vector<StreetNode> nodes = new Vector<StreetNode>();
        JsonArray json_streetNodes = json_streetNetwork.get("nodes").getAsJsonArray();
        for (JsonElement streetNode : json_streetNodes)
        {
            JsonObject json_streetNode = streetNode.getAsJsonObject();
            String name = json_streetNode.get("name").getAsString();
            String company = json_streetNode.get("company").getAsString();
            String guid = json_streetNode.get("guid").getAsString();
            String type = json_streetNode.get("type").getAsString();

            JsonObject json_location = (JsonObject) json_streetNode.get("location");
            Double x = json_location.get("x").getAsDouble();
            Double y = json_location.get("y").getAsDouble();
            Point2D location = new Point2D.Double(x, y);

            StreetNode aStreetNode = new StreetNode(name, company, location, guid, type);
            nodes.add(aStreetNode);
        }

        Vector<StreetLink> links = new Vector<StreetLink>();
        JsonArray json_streetLinks = json_streetNetwork.get("links").getAsJsonArray();
        for (JsonElement streetLink : json_streetLinks)
        {
            JsonObject json_streetLink = streetLink.getAsJsonObject();
            String source = json_streetLink.get("source").getAsString();
            String guid = json_streetLink.get("guid").getAsString();
            double dist = json_streetLink.get("dist").getAsDouble();
            String target = json_streetLink.get("target").getAsString();

            StreetLink aStreetLink = new StreetLink(source, guid, dist, target);
            links.add(aStreetLink);
        }


        StreetNetwork streetNetwork = new StreetNetwork(directed, nodes, links);
        return streetNetwork;
    }

    public static DoughNotification parseDoughNotification(String jsonString)
    {
        JsonElement root = new JsonParser().parse(jsonString);
        JsonObject json_doughNotification = root.getAsJsonObject();

        String productType = json_doughNotification.get("productType").getAsString();
        int quantity = json_doughNotification.get("quantity").getAsInt();
        Vector<String> guids = new Vector<String>();
        JsonArray json_guids = json_doughNotification.get("guids").getAsJsonArray();
        for (JsonElement guid : json_guids)
        {
            guids.add(guid.getAsString());
        }

        DoughNotification doughNotification = new DoughNotification(guids, productType, quantity);
        return doughNotification;
    }

    public static KneadingNotification parseKneadingNotification(String jsonString)
    {
        JsonElement root = new JsonParser().parse(jsonString);
        JsonObject json_kneadingNotification = root.getAsJsonObject();

        String productType = json_kneadingNotification.get("productType").getAsString();
        Vector<String> guids = new Vector<String>();
        JsonArray json_guids = json_kneadingNotification.get("guids").getAsJsonArray();
        for (JsonElement guid : json_guids)
        {
            guids.add(guid.getAsString());
        }

        KneadingNotification kneadingNotification = new KneadingNotification(guids, productType);
        return kneadingNotification;
    }

    public static KneadingRequest parseKneadingRequest(String jsonString)
    {
        JsonElement root = new JsonParser().parse(jsonString);
        JsonObject json_kneadingRequest = root.getAsJsonObject();

        String productType = json_kneadingRequest.get("productType").getAsString();
        Float kneadingTime = json_kneadingRequest.get("kneadingTime").getAsFloat();
        Vector<String> guids = new Vector<String>();
        JsonArray json_guids = json_kneadingRequest.get("guids").getAsJsonArray();
        for (JsonElement guid : json_guids)
        {
            guids.add(guid.getAsString());
        }

        KneadingRequest kneadingRequest = new KneadingRequest(guids, productType, kneadingTime);
        return kneadingRequest;
    }

    public static PreparationNotification parsePreparationNotification(String jsonString)
    {
        JsonElement root = new JsonParser().parse(jsonString);
        JsonObject json_preparationNotification = root.getAsJsonObject();

        String productType = json_preparationNotification.get("productType").getAsString();
        Vector<String> guids = new Vector<String>();
        JsonArray json_guids = json_preparationNotification.get("guids").getAsJsonArray();
        for (JsonElement guid : json_guids)
        {
            guids.add(guid.getAsString());
        }

        PreparationNotification preparationNotification = new PreparationNotification(guids, productType);
        return preparationNotification;
    }

    public static PreparationRequest parsePreparationRequest(String jsonString)
    {
        JsonElement root = new JsonParser().parse(jsonString);
        JsonObject json_preparationRequest = root.getAsJsonObject();

        String productType = json_preparationRequest.get("productType").getAsString();

        Vector<Integer> productQuantities = new Vector<Integer>();
        JsonArray json_productQuantities = json_preparationRequest.get("productQuantities").getAsJsonArray();
        for (JsonElement productQuantity : json_productQuantities)
        {
            productQuantities.add(productQuantity.getAsInt());
        }

        Vector<Step> steps = new Vector<Step>();
        JsonArray json_steps = json_preparationRequest.get("steps").getAsJsonArray();
        for (JsonElement step : json_steps)
        {
            JsonObject json_step = step.getAsJsonObject();
            String action = json_step.get("action").getAsString();
            Float duration = json_step.get("duration").getAsFloat();

            Step aStep = new Step(action, duration);
            steps.add(aStep);
        }

        Vector<String> guids = new Vector<String>();
        JsonArray json_guids = json_preparationRequest.get("guids").getAsJsonArray();
        for (JsonElement guid : json_guids)
        {
            guids.add(guid.getAsString());
        }

        PreparationRequest preparationRequest = new PreparationRequest(guids, productType, productQuantities, steps);
        return preparationRequest;
    }

    public static ProofingRequest parseProofingRequest(String jsonString)
    {
        JsonElement root = new JsonParser().parse(jsonString);
        JsonObject json_proofingRequest = root.getAsJsonObject();

        String productType = json_proofingRequest.get("productType").getAsString();
        Float proofingTime = json_proofingRequest.get("proofingTime").getAsFloat();

        Vector<String> guids = new Vector<String>();
        JsonArray json_guids = json_proofingRequest.get("guids").getAsJsonArray();
        for (JsonElement guid : json_guids)
        {
            guids.add(guid.getAsString());
        }

        ProofingRequest proofingRequest = new ProofingRequest(productType, guids, proofingTime);
        return proofingRequest;
    }
}