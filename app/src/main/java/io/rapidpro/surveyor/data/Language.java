package io.rapidpro.surveyor.data;

import com.neovisionaries.i18n.LanguageAlpha3Code;

/**
 * Created by eric on 9/1/15.
 */
public class Language {

    private String m_iso;
    private String m_name;
    private boolean m_recognized;

    public Language(String iso) {

        m_iso = iso;
        LanguageAlpha3Code code = LanguageAlpha3Code.getByCode(iso);
        if (code != null) {
            m_name = code.getName();
            m_recognized = true;
        } else {
            m_name = "Unknown Language";
            m_recognized = false;
        }
    }

    public boolean isRecognized(){
        return m_recognized;
    }

    public String getName() { return m_name; }

    public String getCode() {
        return m_iso;
    }

    public String toString() {
        return getName();
    }
}
