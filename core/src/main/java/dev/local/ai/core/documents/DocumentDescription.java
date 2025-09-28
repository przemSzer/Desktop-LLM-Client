package dev.local.ai.core.documents;

import java.io.File;

import org.apache.tika.mime.MediaType;

public record DocumentDescription(String title, MediaType type, String text, File file) {

}
