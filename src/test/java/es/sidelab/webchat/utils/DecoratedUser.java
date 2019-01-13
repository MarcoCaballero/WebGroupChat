package es.sidelab.webchat.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Exchanger;

import es.codeurjc.webchat.Chat;
import es.codeurjc.webchat.MessageInfo.MessageType;
import es.codeurjc.webchat.User;
import es.sidelab.webchat.TestUser;

public class DecoratedUser extends TestUser {

	Map<MessageType, CountDownLatch> latches;
	private long delay; // ms
	private Exchanger<String> messageExchanger;


	public DecoratedUser(String name, long delay, Map<MessageType, CountDownLatch> latches) {
		super(name);
		this.delay = delay;
		this.latches = latches;
	}

	public DecoratedUser(String name) {
		this(name, 0, new ConcurrentHashMap<>());
	}

	public DecoratedUser latchEntry(CountDownLatch latch) {
		setLatch(MessageType.ENTRY, latch);
		return this;
	}

	public DecoratedUser latchExit(CountDownLatch latch) {
		setLatch(MessageType.EXIT, latch);
		return this;
	}

	public DecoratedUser latchMessage(CountDownLatch latch) {
		setLatch(MessageType.MSG, latch);
		return this;
	}

	public DecoratedUser delayUser(long delay) {
		this.delay = delay;
		return this;
	}

	public DecoratedUser messageExchanger(Exchanger<String> exchanger) {
		this.messageExchanger = exchanger;
		return this;
	}


	@Override
	public void newUserInChat(Chat chat, User user) {
		super.newUserInChat(chat, user);
		CountDownLatch entryLatch = latches.get(MessageType.ENTRY);
		if (entryLatch != null)
			delayCountDown(entryLatch, delay);
	}

	@Override
	public void userExitedFromChat(Chat chat, User user) {
		super.userExitedFromChat(chat, user);
		CountDownLatch exitLatch = latches.get(MessageType.EXIT);
		if (exitLatch != null)
			delayCountDown(exitLatch, delay);
	}

	@Override
	public void newMessage(Chat chat, User user, String message) {
		super.newMessage(chat, user, message);
		CountDownLatch msgLatch = latches.get(MessageType.MSG);
		if (msgLatch != null)
			delayCountDown(msgLatch, delay);
		if (messageExchanger != null)
			exchangeMessage(message);
		
	}

	private void delayCountDown(CountDownLatch latch, long delay) {
		try {
			Thread.sleep(delay);
		} catch (InterruptedException e) {
		} finally {
			latch.countDown();
		}
	}

	private void setLatch(MessageType type, CountDownLatch latch) {
		latches.putIfAbsent(type, latch);
	}

	private void exchangeMessage(String message){
		try{
			messageExchanger.exchange(message);
		} catch (InterruptedException e) {}
	}
}
