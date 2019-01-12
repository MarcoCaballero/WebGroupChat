package es.sidelab.webchat;

import static org.junit.Assert.fail;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.Test;
import es.codeurjc.webchat.ChatManager;
import es.sidelab.webchat.utils.ConcurrentPrinter;
import es.sidelab.webchat.utils.ConnectionTask;
import es.sidelab.webchat.utils.TestTaskResult;

public class ConcurrentBehaviorTest {

    private static final ConcurrentPrinter printer = ConcurrentPrinter.getInstance();

    @Test
    public void connected_users_creating_chats_and_joining_to_it_test() {
        final int MAX_USERS = 4;
        final int MAX_ITERATIONS_PER_USER = 5;
        ExecutorService WORKER_THREAD_POOL = Executors.newFixedThreadPool(4);
        CompletionService<TestTaskResult> executor =
                new ExecutorCompletionService<>(WORKER_THREAD_POOL);
        ChatManager chatManager = new ChatManager(50);

        List<ConnectionTask> callables = new ArrayList<>();
        for (int i = 0; i < MAX_USERS; i++) {
            callables.add(new ConnectionTask(i, chatManager, MAX_ITERATIONS_PER_USER));
        }

        for (ConnectionTask callable : callables) {
            executor.submit(callable);
            printer.println("\n Callable task: " + callable.getId() + " submitted to executor \n");
        }

        List<TestTaskResult> results = new ArrayList<>();

        for (int i = 0; i < MAX_USERS; i++) {
            callables.add(new ConnectionTask(i, chatManager, MAX_ITERATIONS_PER_USER));
            try {
                Future<TestTaskResult> future = executor.take();
                results.add(future.get());
            } catch (InterruptedException e) {
                // e.printStackTrace();
                fail("InterruptedException");
            } catch (ExecutionException e) {
                // e.printStackTrace();
                fail("ExecutionException: ConcurrentModificationException");
            }
        }

        printer.println("EXPCETIONS: \n");
        printer.printWithTaskSignature(results, "Result: ");

        results.forEach((elem) -> {
            if (!elem.isFinished())
                fail(); // Not fisnished task due to Exception
        });
    }
}
