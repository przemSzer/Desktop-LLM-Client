package dev.local.ai.ui.chat.controls;

import javafx.scene.Cursor;
import javafx.scene.Node;
import one.jpro.platform.mdfx.MarkdownView;
import dev.local.ai.ui.utils.HostServicesProvider;

public class AIMessageMarkdownView extends MarkdownView {

    public AIMessageMarkdownView() {
        super();
        
    }

    @Override
    public void setLink(Node node, String link, String description) {
        node.setCursor(Cursor.HAND);
        node.setOnMouseClicked(e -> HostServicesProvider.getInstance().getHostServices().showDocument(link));
    }


}
