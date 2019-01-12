package es.sidelab.webchat.utils;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import es.codeurjc.webchat.Chat;
import es.codeurjc.webchat.ChatManager;
import es.sidelab.webchat.TestUser;

public class ConnectionTask implements Callable<TestTaskResult> {

        private long id;
        private int iterations;
        private ChatManager chatManager;

        public ConnectionTask(long id, ChatManager chatManager, int iterations) {
            this.id = id;
            this.chatManager = chatManager;
            this.iterations = iterations;
        }

        public String getUserId() {
            return "User_" + this.id;
        }

        public long getId() {
            return id;
        }

        public TestTaskResult call() throws InterruptedException, TimeoutException, ExecutionException {
            return createChatsAndInsertUser();
        }

        private TestTaskResult createChatsAndInsertUser() throws InterruptedException, TimeoutException, ExecutionException {
            TestTaskResult result = new TestTaskResult(id);
            TestUser user = new TestUser(getUserId());

            for (int i = 0; i < this.iterations; i++) {
                String chatId = "chat_" + i;
                Chat chat = chatManager.newChat(chatId, 5, TimeUnit.SECONDS);
                chat.addUser(user);
                // ConcurrentPrinter.getInstance().printWithTaskSignature(chat.getUsers(), "User: ",
                // this.id); // CARE GENERATES FALSE POSITIVE, GOOD FOR DEBUG
            }

            ConcurrentPrinter.getInstance().printEndWithTaskSignature(this.id);
            result.checkFinished();

            return result;
        }
    }