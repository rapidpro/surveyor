/*
 * RapidPro Android Channel - Relay SMS messages where MNO connections aren't practical.
 * Copyright (C) 2014 Nyaruka, UNICEF
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.rapidpro.surveyor.ui;

import android.content.Context;
import android.graphics.Typeface;

import java.util.Hashtable;

import io.rapidpro.surveyor.Logger;

public class Typefaces {

    private static final Hashtable<String, Typeface> s_fontCache = new Hashtable<String, Typeface>();

    public static Typeface get(Context c, String assetPath) {
        synchronized (s_fontCache) {
            if (!s_fontCache.containsKey(assetPath)) {
                try {
                    Typeface t = Typeface.createFromAsset(c.getAssets(), assetPath);
                    s_fontCache.put(assetPath, t);
                } catch (Exception e) {
                    Logger.e("Could not get typeface '" + assetPath + "' because " + e.getMessage(), e);
                    return null;
                }
            }
            return s_fontCache.get(assetPath);
        }
    }
}
