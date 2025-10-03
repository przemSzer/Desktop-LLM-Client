package dev.local.ai.core.chat.streaming;

public interface IPartialMessageAware {

    void setPartialMessageListener(IPartialMessagesListener listener);

}
