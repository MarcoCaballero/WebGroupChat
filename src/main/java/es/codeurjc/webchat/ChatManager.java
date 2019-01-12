package es.codeurjc.webchat;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ChatManager {

	private ReadAndWriteLock guard;

	private Map<String, Chat> chats = new ConcurrentHashMap<>();
	private Map<String, User> users = new ConcurrentHashMap<>();
	private int maxChats;

	public ChatManager(int maxChats) {
		this.maxChats = maxChats;
		guard = new ReadAndWriteLock();
	}

	public void newUser(User user) {
		if (isUserRegisteredYet(user.getName())) {
			throw getUserRegisteredYetException(user.getName());
		}else {
			addUser(user);
		}
	}

	public Chat newChat(String name, long timeout, TimeUnit unit) throws InterruptedException,
			TimeoutException {

		if (isMaxChatsCapacityFull()) { 
			throw getMaxChatsCapacityException(); 
		}

		return (isChatCreatedYet(name)) ? getChat(name) : createChat(name);
	}

	public void closeChat(Chat chat) {
		Chat removedChat = removeChat(chat.getName());
		if (removedChat == null) {
			throw getCloseConnectionUnknownChatException(chat.getName());
		}

		guard.read(() -> {
			getUsers().stream()
				.forEach((user) -> user.chatClosed(removedChat));
		});
	}

	public Collection<Chat> getChats() {
		return Collections.unmodifiableCollection(chats.values());
	}

	public Chat getChat(String chatName) {
		return guard.read(()-> chats.get(chatName));
	}

	public Chat removeChat(String chatName) {
		return guard.read(()-> chats.remove(chatName));
	}

	public User getUser(String userName) {
		return guard.read(()-> users.get(userName));
	}

	public Collection<User> getUsers() {
		return Collections.unmodifiableCollection(users.values());
	}

	public void close() {}

	private void addUser(User user) {
		guard.write(() -> {
			users.put(user.getName(), user);
		});
	}

	private Chat createChat(String chatName) {
		Chat newChat = new Chat(this, chatName);

		guard.write(() -> chats.put(chatName, newChat));

		guard.read(() -> {
			getUsers().stream()
				.forEach((user) -> user.newChat(newChat));
		});

		return newChat;
	}

	private boolean isChatCreatedYet(String chatName) {
		return guard.read(() -> chats.containsKey(chatName));
	}

	private boolean isUserRegisteredYet(String userName) {
		return guard.read(() -> users.containsKey(userName));
	}

	private boolean isMaxChatsCapacityFull() {
		return guard.read(() -> chats.size() == maxChats);
	}

	private IllegalArgumentException getUserRegisteredYetException(String userName) {
		return new IllegalArgumentException("There is already a user with name \'" + userName + "\'");
	}

	private IllegalArgumentException getCloseConnectionUnknownChatException(String chatName) {
		return new IllegalArgumentException("Trying to remove an unknown chat with name \'" + chatName + "\'");
	}

	private TimeoutException getMaxChatsCapacityException() {
		return new TimeoutException("There is no enought capacity to create a new chat");
	}
}
