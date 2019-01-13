package es.sidelab.webchat;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.Exchanger;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.runners.MethodSorters;


import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;

import es.codeurjc.webchat.Chat;
import es.codeurjc.webchat.ChatManager;
import es.codeurjc.webchat.User;
import es.sidelab.webchat.utils.ConcurrentPrinter;
import es.sidelab.webchat.utils.DecoratedUser;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class BehaviourTest {
    private static final ConcurrentPrinter printer = ConcurrentPrinter.getInstance();

    private static final String CHAT_NAME = "CHAT_SuT";
    private static final int MAX_USERS = 10;

    private static ArrayList<Exchanger<String>> exchangers ;

    private static ChatManager chatManager;
    private static Chat chat;

    @BeforeClass
    public static void before() {
        chatManager = new ChatManager(1);
        exchangers = new ArrayList<Exchanger<String>>();
    }

    @AfterClass
    public static void after() {
        chatManager.close();
        chatManager = null;
        chat = null;
    }

	@Test
	public void phase1_new_chat() throws InterruptedException, TimeoutException {

        printer.println("\n phase1_new_chat (Number of users: " + MAX_USERS + " ): \n");

        try{
            chat = chatManager.newChat(CHAT_NAME, 10 , TimeUnit.SECONDS);
        } catch (InterruptedException | TimeoutException e) {
            fail("Test rejected by Handling exception on chat creation: \n" + e.getCause());
        }
        String chat_name = chat.getName();

		assertTrue("The chat name should be" + CHAT_NAME +" but the value is :"
				+ chat_name, Objects.equals(chat_name, CHAT_NAME));
	}

	@Test
	public void phase2_new_users_in_chat() throws InterruptedException, TimeoutException {

        printer.println("\n phase2_new_users_in_chat (Number of users: " + MAX_USERS + " ): \n");

        try{
            for (int i = 0; i < MAX_USERS; i++) {
                String userName = "USER_" + i;
                Exchanger<String> exchanger = new Exchanger<>();
                exchangers.add(exchangers.size(), exchanger);
                User user =  new DecoratedUser(userName)
                                    .messageExchanger(exchanger);
                chatManager.newUser(user);
                chat.addUser(user);
                for (int n = 0; n<i; n++){
                    String newUserReceived = exchangers.get(n).exchange(null);
                    assertTrue("New user " + userName + ", does not correspond with user received " + newUserReceived,
                    userName.equals(newUserReceived));
                }
            }
        } catch (InterruptedException e) {
            fail("Test rejected by Handling exception on adding users on chat: \n" + e.getCause());
        }
    }

    @Test
    public void phase3_greetings_users_chat() throws InterruptedException, TimeoutException {

        printer.println("\n phase3_greetings_users_chat (Number of users: " + MAX_USERS + " ): \n");

        try{
            for (int i = 0; i < MAX_USERS; i++) {
                String userName = "USER_" + i;
                String message = "Hi! I'm " + userName;
                User user = chat.getUser(userName);
                chat.sendMessage(user, message);
                for (int n = 0; n< MAX_USERS; n++){
                        String messageReceived = exchangers.get(n).exchange(null);
                        assertTrue("Message received ' " + message + " ', does not correspond with message received " + messageReceived,
                        messageReceived.equals(message));
                }
            }
        } catch (InterruptedException e) {
            fail("Test rejected by Handling exception on messages exchange: \n" + e.getCause());
        }
    }    
    
    @Test
    public void phase4_exit_users_in_chat() throws InterruptedException, TimeoutException {

        printer.println("\n phase4_exit_users_in_chat (Number of users: " + MAX_USERS + " ): \n");
        
        try{
            for (int i = 0; i < MAX_USERS; i++) {
                String userName = "USER_" + i;
                User user = chat.getUser(userName);
                chat.removeUser(user);
                for (int n = i + 1; n< MAX_USERS; n++){
                    String exitUserReceived = exchangers.get(n).exchange(null);
                    assertTrue("User exited " + userName + ", does not correspond with user received " + exitUserReceived,
                    userName.equals(exitUserReceived));
                }
            }
        } catch (InterruptedException e) {
            fail("Test rejected by Handling exception on users exit: \n" + e.getCause());
        }
    }
    
    @Test
    public void phase5_close_chat() throws InterruptedException, TimeoutException {
            printer.println("\n phase5_close_chat (Number of users: " + MAX_USERS + " ): \n");
            chatManager.closeChat(chat);
            Chat currentChat = chatManager.getChat(CHAT_NAME);
            assertNull("Chat ' " + CHAT_NAME + " 'not deleted.",
            currentChat);
    }

}