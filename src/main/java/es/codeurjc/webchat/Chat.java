package es.codeurjc.webchat;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import es.codeurjc.webchat.MessageInfo;
import es.codeurjc.webchat.MessageInfo.MessageType;

public class Chat {
	private static final int MSG_BUFFER_THRESHOLD = 20;
	private ReadAndWriteLock guard;

	private String name;
	private Map<String, User> users = new ConcurrentHashMap<>();
	private Map<String, BlockingQueue<MessageInfo>> messageMap = new ConcurrentHashMap<>();

	private ChatManager chatManager;

	public Chat(ChatManager chatManager, String name) {
		this.chatManager = chatManager;
		this.name = name;
		guard = new ReadAndWriteLock();
	}

	public String getName() {
		return guard.read(() -> name);
	}

	public Collection<User> getUsers() {
		return guard.read(() -> Collections.unmodifiableCollection(users.values()));
	}

	public User getUser(String name) {
		return guard.read(() -> users.get(name));
	}

	public void addUser(User user) {
		guard.write(() -> {
			final String name = user.getName();
			users.put(name, user);
			if (!messageMap.containsKey(name)) {
				messageMap.put(name, new ArrayBlockingQueue<>(MSG_BUFFER_THRESHOLD));
				new Thread(() -> dispatchMessage(messageMap.get(name), user)).start();
			}
		});
		alertOtherUsersNewUserInChat(user);
	}

	public void removeUser(User user) {
		guard.write(() -> users.remove(user.getName()));
		alertOtherUsersExitFromChat(user);
	}

	public void sendMessage(User user, String message) {
		guard.read(() -> {
			getUsers()
				.forEach((us) -> addMessagetoQueue(us.getName(), new MessageInfo(MessageType.MSG, user, message)));
		});
	}

	public void close() {
		guard.write(() -> chatManager.closeChat(this));
	}

	private void alertOtherUsersNewUserInChat(User user) {
		guard.read(() -> {
			getUsers()
					.stream()
					.filter((us) -> us != user)
					.forEach((us) -> addMessagetoQueue(us.getName(), new MessageInfo(MessageType.ENTRY, user)));
		});
	}

	private void alertOtherUsersExitFromChat(User user) {
		guard.read(() -> {
			getUsers()
				.forEach((us) -> addMessagetoQueue(us.getName(), new MessageInfo(MessageType.EXIT, user)));
		});
	}

	private void addMessagetoQueue(String userName, MessageInfo info) {
		try {
			messageMap.get(userName).put(info);
		} catch (InterruptedException e) {
		}
	}

	private void dispatchMessage(BlockingQueue<MessageInfo> queue, User userTo) {
		while (true) {
			try {
				MessageInfo msgInfo = queue.take();
				switch (msgInfo.getType()) {
					case ENTRY:
						userTo.newUserInChat(this, msgInfo.getUserFrom());
						break;
					case EXIT:
						userTo.userExitedFromChat(this, msgInfo.getUserFrom());
						break;
					default:
						userTo.newMessage(this, msgInfo.getUserFrom(), msgInfo.getMessage());
						break;
				}
			} catch (InterruptedException e) {
			}
		}
	}
}
