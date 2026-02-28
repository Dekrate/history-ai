package com.historyai.dto;

/**
 * Data transfer object representing the result of a fact-check verification.
 * Contains the claim, verification status, source, explanation, and confidence score.
 */
public class FactCheckResult {

    private String claim;
    private VerificationResult verification;
    private String source;
    private String explanation;
    private Float confidence;

    /**
     * Enumeration of possible verification results.
     */
    public enum VerificationResult {
        /** The claim has been verified as true */
        VERIFIED,
        /** The claim has been verified as false */
        FALSE,
        /** The claim could not be verified due to lack of information */
        UNVERIFIABLE,
        /** The claim is partially true but has inaccuracies */
        PARTIAL
    }

    public FactCheckResult() {
    }

    /**
     * Constructs a new FactCheckResult with all fields.
     *
     * @param claim the original claim that was verified
     * @param verification the verification result
     * @param source the source of verification (e.g., Wikipedia)
     * @param explanation explanation of the verification decision
     * @param confidence confidence score between 0.0 and 1.0
     */
    public FactCheckResult(String claim, VerificationResult verification, String source, 
                           String explanation, Float confidence) {
        this.claim = claim;
        this.verification = verification;
        this.source = source;
        this.explanation = explanation;
        this.confidence = confidence;
    }

    public String getClaim() {
        return claim;
    }

    public void setClaim(String claim) {
        this.claim = claim;
    }

    public VerificationResult getVerification() {
        return verification;
    }

    public void setVerification(VerificationResult verification) {
        this.verification = verification;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public Float getConfidence() {
        return confidence;
    }

    public void setConfidence(Float confidence) {
        this.confidence = confidence;
    }
}
