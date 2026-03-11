package com.historyai.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for fact-checking endpoint.
 *
 * <p>Contains the message to be verified along with optional historical character context.
 * The character information is used to fetch relevant Wikipedia data for more accurate
 * fact-checking against the specific historical figure's biography.</p>
 *
 * <p>Example usage:</p>
 * <pre>
 * FactCheckRequest request = new FactCheckRequest(
 *     "Mikołaj Kopernik urodził się w 1473 roku",
 *     "Mikołaj Kopernik",
 *     "Polski astronom i matematyk..."
 * );
 * </pre>
 *
 * @author HistoryAI Team
 * @version 1.0
 * @see FactCheckResult
 * @see com.historyai.controller.FactCheckController
 */
public class FactCheckRequest {

    /** The message containing claims to be verified */
    @NotBlank(message = "Message must not be blank")
    private String message;
    
    /** Name of the historical character for Wikipedia lookup */
    private String characterName;
    
    /** Optional context about the historical character (biography, era, etc.) */
    private String characterContext;

    public FactCheckRequest() {
    }

    public FactCheckRequest(String message, String characterName, String characterContext) {
        this.message = message;
        this.characterName = characterName;
        this.characterContext = characterContext;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCharacterName() {
        return characterName;
    }

    public void setCharacterName(String characterName) {
        this.characterName = characterName;
    }

    public String getCharacterContext() {
        return characterContext;
    }

    public void setCharacterContext(String characterContext) {
        this.characterContext = characterContext;
    }
}
