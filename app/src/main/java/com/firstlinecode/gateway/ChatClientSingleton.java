package com.firstlinecode.gateway;

import com.firstlinecode.chalk.IChatClient;
import com.firstlinecode.chalk.android.StandardChatClient;

public class ChatClientSingleton {
    private static IChatClient chatClient;

    public static IChatClient get() {
        if (chatClient == null)
            chatClient = new StandardChatClient(Toolkits.getStreamConfig(App.getAppContext()));

        return chatClient;
    }
}
