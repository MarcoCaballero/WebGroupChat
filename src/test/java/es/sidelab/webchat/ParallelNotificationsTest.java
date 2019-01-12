package es.sidelab.webchat;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
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
import es.codeurjc.webchat.MessageInfo.MessageType;
import es.codeurjc.webchat.User;
import es.sidelab.webchat.utils.ConcurrentPrinter;
import es.sidelab.webchat.utils.DecoratedUser;

@RunWith(Parameterized.class)
public class ParallelNotificationsTest {
    private static final ConcurrentPrinter printer = ConcurrentPrinter.getInstance();

    private static final long DELAY_TIME = 1000; // ms
    private static final long ERR_THRESHOLD_ENTRY = DELAY_TIME * 9 / 5 + 399; // 1000 * 9 / 5 + 399 => 2199ms
    private static final long ERR_THRESHOLD_EXIT = ERR_THRESHOLD_ENTRY + 900; // 2199ms + 800 => 3099ms
    private static final String CHAT_NAME = "CHAT_SuT";

    @Parameters
    public static Iterable<? extends Object> data() {
        return Arrays.asList(4, 10, 11);
    }

    @Parameter
    public int MAX_USERS;


    Map<MessageType, CountDownLatch> latches = new ConcurrentHashMap<>();
    ChatManager chatManager;
    Chat chat;

    @Before
    public void beforeEach() {
        try {
            latches.put(MessageType.ENTRY, new CountDownLatch(MAX_USERS));
            latches.put(MessageType.EXIT, new CountDownLatch(MAX_USERS));
            latches.put(MessageType.MSG, new CountDownLatch(MAX_USERS));
            chatManager = new ChatManager(50);
            chat = chatManager.newChat(CHAT_NAME, 10, TimeUnit.SECONDS);
        } catch (InterruptedException | TimeoutException e) {
            fail("Test rejected by Handling exception on beforeEach: \n" + e.getCause());
        }
    }

    @After
    public void afterEach() {
        latches.clear();
        chatManager.closeChat(chat);
        chatManager.close();
        chatManager = null;
        chat = null;
    }

    @Test
    public void users_sending_new_user_in_chat_in_parallel_test() {

        printer.println("\nusers_sending_new_user_in_chat_in_parallel_test (Users: " + MAX_USERS + " ): \n");

        try {
            for (int i = 0; i < MAX_USERS; i++) {
                User user =  new DecoratedUser("USER_" + i)
                                    .latchEntry(latches.get(MessageType.ENTRY))
                                    .delayUser(DELAY_TIME);
                chatManager.newUser(user);
            }

            chatManager.getUsers()
                .forEach(chat::addUser);

            assertTrue("Latch has not been waiting the proper time to add all users",
                latches.get(MessageType.ENTRY).await(ERR_THRESHOLD_ENTRY, TimeUnit.MILLISECONDS));

        } catch (InterruptedException  e) {
            fail("Test rejected by Handling exception in users_sending_new_user_in_chat_in_parallel_test: \n" + e.getCause());
        }
    }

    @Test
    public void users_sending_user_exited_from_chat_in_parallel_test() {

        printer.println("\nusers_sending_user_exited_from_chat_in_parallel_test (Users: " + MAX_USERS + " ): \n");
        CountDownLatch entryLatch = latches.get(MessageType.ENTRY);
        CountDownLatch exitLatch = latches.get(MessageType.EXIT);
        try {
            for (int i = 0; i < MAX_USERS; i++) {
                User user =  new DecoratedUser("USER_" + i)
                                    .latchEntry(entryLatch)
                                    .latchExit(exitLatch)
                                    .delayUser(DELAY_TIME);
                chatManager.newUser(user);
            }

            chatManager.getUsers()
                .forEach(chat::addUser);

            assertTrue("Latch has not been waiting the proper time to add all users",
                entryLatch.await(ERR_THRESHOLD_ENTRY, TimeUnit.MILLISECONDS));
            
            chatManager.getUsers()
                .forEach(chat::removeUser);

            assertTrue("Latch has not been waiting the proper time to exit all users",
                    exitLatch.await(ERR_THRESHOLD_EXIT, TimeUnit.MILLISECONDS));

        } catch (InterruptedException  e) {
            fail("Test rejected by Handling exception in users_sending_new_user_in_chat_in_parallel_test: \n" + e.getCause());
        }
    }
}
