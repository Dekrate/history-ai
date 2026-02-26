package com.historyai.exception;

public class CharacterNotFoundInWikipediaException extends RuntimeException {

    public CharacterNotFoundInWikipediaException(String characterName) {
        super("Character not found in Wikipedia: " + characterName);
    }
}
