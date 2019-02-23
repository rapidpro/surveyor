package io.rapidpro.surveyor.net.responses;

import java.util.List;

public class TokenResults {

    private List<Token> tokens;

    public List<Token> getTokens() {
        return tokens;
    }

    /**
     * Gets the tokens as a simple array of token strings
     *
     * @return the array of tokens
     */
    public String[] toRawTokens() {
        String[] raw = new String[this.tokens.size()];
        for (int t = 0; t < this.tokens.size(); t++) {
            raw[t] = this.tokens.get(t).getToken();
        }
        return raw;
    }
}
