package es.sidelab.webchat;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Exchanger;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import es.codeurjc.webchat.Chat;
import es.codeurjc.webchat.ChatManager;
    import es.codeurjc.webchat.User;
import es.sidelab.webchat.utils.ConcurrentPrinter;
import es.sidelab.webchat.utils.DecoratedUser;

@RunWith(Parameterized.class)
public class MessageOrderTest {
    private static final ConcurrentPrinter printer = ConcurrentPrinter.getInstance();

    private static final String CHAT_NAME = "CHAT_SuT";

    private Exchanger<String> exchanger = new Exchanger<>();

    @Parameters
    public static Iterable<? extends Object> data() {
        return Arrays.asList(4, 10, 11);
    }

    @Parameter
    public int MESSAGES;


    CountDownLatch messageLatch ;
    ChatManager chatManager;
    Chat chat;

    @Before
    public void before() {
        try {
            messageLatch = new CountDownLatch(MESSAGES);
            chatManager = new ChatManager(1);
            chat = chatManager.newChat(CHAT_NAME, 10, TimeUnit.SECONDS);
        } catch (InterruptedException | TimeoutException e) {
            fail("Test rejected by Handling exception on beforeEach: \n" + e.getCause());
        }
    }

    @After
    public void after() {
        chatManager.closeChat(chat);
        chatManager.close();
        chatManager = null;
        chat = null;
    }

    @Test
    public void user_sending_ordered_messages_test() {

        printer.println("\nuser_sending_ordered_messages_test (Number of messages: " + MESSAGES + " ): \n");
        
        try {
            User sender =  new TestUser("SENDER");
            User receiver =  new DecoratedUser("RECEIVER")
            .latchMessage(messageLatch)
            .messageExchanger(exchanger);
            chatManager.newUser(sender);
            chat.addUser(sender);
            chatManager.newUser(receiver);
            chat.addUser(receiver);
            for (int messageSent = 0; messageSent < MESSAGES; messageSent++) {
                chat.sendMessage(sender, String.valueOf(messageSent));
                int messageReceived = Integer.valueOf(exchanger.exchange(null));
                assertTrue("Message sent " + messageSent + ", does not correspond with message received " + messageReceived,
                messageSent == messageReceived);
            }
        } catch (InterruptedException  e) {
            fail("Test rejected by Handling exception in user_sending_ordered_messages_test: \n" + e.getCause());
        }
    }
}
