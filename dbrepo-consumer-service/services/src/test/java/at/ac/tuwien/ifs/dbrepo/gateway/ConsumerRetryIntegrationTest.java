package at.ac.tuwien.ifs.dbrepo.gateway;

import at.ac.tuwien.ifs.dbrepo.config.RabbitConfig;
import at.ac.tuwien.ifs.dbrepo.gateway.impl.DataServiceGatewayImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class ConsumerRetryIntegrationTest {
    @Container static RabbitMQContainer broker=new RabbitMQContainer("rabbitmq:3-management")
            .withAdminUser("fixture-internal").withAdminPassword("fixture-password");

    @Test void recoversBeforeAckAndRetainsExhaustedOrInvalidMessages() throws Exception {
        ObjectMapper json=new ObjectMapper();
        CachingConnectionFactory factory=new CachingConnectionFactory(broker.getHost(),broker.getAmqpPort());
        factory.setUsername(broker.getAdminUsername());factory.setPassword(broker.getAdminPassword());factory.setVirtualHost("/");
        RabbitTemplate rabbit=new RabbitTemplate(factory);
        rabbit.execute(channel->{channel.exchangeDeclare("dbrepo","topic",true);channel.queueDeclare("dbrepo",true,false,false,Map.of("x-queue-type","quorum"));channel.queueBind("dbrepo","dbrepo","dbrepo.#");return null;});
        String routing="dbrepo."+UUID.randomUUID()+"."+UUID.randomUUID();
        publish(rabbit,routing,"{\"id\":1}",true);
        Path config=Files.createTempFile("broker-error-check-",".json");
        try {
            Files.writeString(config,json.writeValueAsString(Map.of("endpoint",broker.getHttpUrl(),"username",broker.getAdminUsername(),"password",broker.getAdminPassword(),"vhost","/")));
            for (int repeat=0; repeat<2; repeat++) {
                var result=new ProcessBuilder(System.getenv().getOrDefault("DBREPO_TEST_PYTHON","python3"),
                        Path.of("../../.scripts/configure-consumer-errors.py").toAbsolutePath().normalize().toString(),"--config",config.toString())
                        .redirectErrorStream(true).start();
                String output=new String(result.getInputStream().readAllBytes(),StandardCharsets.UTF_8);
                assertEquals(0,result.waitFor(),"Compatible error-topology setup failed: "+output);
            }
            Integer pending=rabbit.execute(channel->channel.queueDeclarePassive("dbrepo").getMessageCount());
            assertEquals(1,pending.intValue());
        } finally {Files.deleteIfExists(config);}
        Map<Integer,AtomicInteger> attempts=new ConcurrentHashMap<>();
        List<Integer> inserted=new CopyOnWriteArrayList<>();
        List<String> identities=new CopyOnWriteArrayList<>();
        HttpServer backend=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        backend.createContext("/api/v1/database",exchange->{
            Map<?,?> body=json.readValue(exchange.getRequestBody(),Map.class);
            int id=((Number)((Map<?,?>)body.get("data")).get("id")).intValue();
            int attempt=attempts.computeIfAbsent(id,key->new AtomicInteger()).incrementAndGet();
            identities.add(exchange.getRequestHeaders().getFirst("X-DBRepo-Publisher"));
            int status=id==4?403:(id==2 || attempt==1)?503:201;
            if(status==201)inserted.add(id);
            exchange.sendResponseHeaders(status,-1);exchange.close();
        });
        backend.start();
        RestTemplate http=new RestTemplate();http.setUriTemplateHandler(new DefaultUriBuilderFactory("http://127.0.0.1:"+backend.getAddress().getPort()));
        RabbitConfig settings=new RabbitConfig(json,new DataServiceGatewayImpl(http));
        ReflectionTestUtils.setField(settings,"queueName","dbrepo");ReflectionTestUtils.setField(settings,"minConcurrent",1);
        ReflectionTestUtils.setField(settings,"maxConcurrent",1);
        ReflectionTestUtils.setField(settings,"connectionTimeout",100);
        var listener=settings.container(factory);
        try {
            listener.start();
            waitUntil(()->inserted.size()==1);
            assertEquals(List.of(1),inserted);
            assertEquals(2,attempts.get(1).get());
            publish(rabbit,routing,"{\"id\":2}",true);
            Message exhausted=receive(rabbit);
            assertEquals("{\"id\":2}",new String(exhausted.getBody(),StandardCharsets.UTF_8));
            assertEquals(3,attempts.get(2).get());
            assertEquals(broker.getAdminUsername(),exhausted.getMessageProperties().getReceivedUserId());
            assertTrue(exhausted.getMessageProperties().getHeaders().containsKey("x-death"));
            publish(rabbit,routing,"not-json",true);
            assertEquals("not-json",new String(receive(rabbit).getBody(),StandardCharsets.UTF_8));
            publish(rabbit,routing,"{\"id\":3}",false);
            assertEquals("{\"id\":3}",new String(receive(rabbit).getBody(),StandardCharsets.UTF_8));
            publish(rabbit,routing,"{\"id\":4}",true);
            assertEquals("{\"id\":4}",new String(receive(rabbit).getBody(),StandardCharsets.UTF_8));
            assertEquals(1,attempts.get(4).get());
            assertEquals(List.of(1),inserted);
            assertEquals(3,attempts.size());
            assertTrue(identities.stream().allMatch(broker.getAdminUsername()::equals));
        } finally {listener.stop();backend.stop(0);factory.destroy();}
    }

    private static void publish(RabbitTemplate rabbit,String routing,String body,boolean identity) {
        MessageProperties properties=new MessageProperties();properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        if(identity)properties.setUserId(broker.getAdminUsername());
        rabbit.send("dbrepo", routing,new Message(body.getBytes(StandardCharsets.UTF_8),properties));
    }
    private static Message receive(RabbitTemplate rabbit) throws Exception {
        final Message[] value=new Message[1];waitUntil(()->{value[0]=rabbit.receive("dbrepo.consumer.errors");return value[0]!=null;});return value[0];
    }
    private static void waitUntil(java.util.function.BooleanSupplier ready) throws Exception {
        long end=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(20);
        while(System.nanoTime()<end){if(ready.getAsBoolean())return;Thread.sleep(50);}
        fail("Timed out waiting for native broker processing");
    }
}
